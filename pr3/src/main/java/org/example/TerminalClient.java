package org.example;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static org.example.RegistryServer.REGISTRY_HOST;
import static org.example.RegistryServer.REGISTRY_PORT;

public class TerminalClient implements Runnable {

    private static final int RING_UPDATE_INTERVAL_MS = 5000;

    private final String name;
    private final String ip;
    private final int port;
    private int clientId;
    private int prevId, nextId;
    private boolean hasToken;

    private final Scanner scanner = new Scanner(System.in);
    private final SocketClient socketClient = new SocketClient();

    public TerminalClient(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        hasToken = false;
    }

    public static void main(String[] args) throws Exception {
        TerminalClient client = new TerminalClient("client1", "127.0.0.1", 10000);
        client.start();
    }

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        //Registrierung beim Registry-Server
        if (!register()) {
            System.err.println("Registrierung fehlgeschlagen, beende.");
            return;
        }

        //Token-Listener starten (um eingehende Token-Requests zu empfangen)
        startTokenListener(port);

        //Ring-Update starten
        new Thread(this::updateRingInfo).start();

        // 4. Falls erster Client: Token starten
        List<Map<String,Object>> clients = listClients();

        if (!clients.isEmpty()) {
            // ID als Number auslesen und in int umwandeln
            Number firstIdNumber = (Number) clients.get(0).get("id");
            int firstClientId = firstIdNumber.intValue();

            if (clientId == firstClientId) {
                System.out.println("Ich bin der erste Client - starte Token.");
                hasToken = true;
                controlRobotWithToken();
                sendToken();
            }
        }


        while (true) {
            System.out.println("\n--- Terminal Client ---");
            System.out.println("1: Liste Roboter anzeigen");
            System.out.println("2: Roboter auswaehlen und steuern");
            System.out.println("0: Beenden");
            System.out.print("Eingabe: ");
            String input = scanner.nextLine();

            switch (input) {
                case "1" -> listRobots();
                case "2" -> {
                    if (hasToken) {
                        controlRobotWithToken();
                    } else {
                        System.out.println("Du hast kein Token - Robotersteuerung nicht erlaubt.");
                    }
                }
                case "0" -> {
                    unregister();
                    return;
                }
                default -> System.out.println("Ungueltige Eingabe");
            }
        }
    }

    private boolean register() {
        try {


            Entry entry = new Entry(
                    -1,              // ID wird vom Server vergeben
                    name,
                    ip,
                    port,
                    Entry.Type.CLIENT
            );

            Request<OperationRegistry, Entry> req =
                    new Request<>(OperationRegistry.REGISTER, entry);

            //out.println(gson.toJson(req));

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, req);

            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                clientId = ((Number)p.get("id")).intValue();
                System.out.println("Registrierung erfolgreich, Client-ID: " + clientId);
                return true;
            } else {
                System.err.println("Registrierung fehlgeschlagen: " + response.get("message"));
                return false;
            }

        } catch (Exception e) {
            System.err.println("Registry nicht erreichbar: " + e.getMessage());
            return false;
        }
    }

    private void unregister() {
        try {

            Request<OperationRegistry, String> request =
                    new Request<>(OperationRegistry.UNREGISTER, name);

            socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);
            System.out.println("Unregistrierung gesendet.");
        } catch (Exception e) {
            System.err.println("Fehler beim Unregister: " + e.getMessage());
        }
    }

    private List<Map<String,Object>> listClients() {
        try {
            Request<OperationRegistry, Entry.Type> request =
                    new Request<>(OperationRegistry.LIST, Entry.Type.CLIENT);

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            if (!("ok".equals(response.get("status")))) {
                System.err.println("Fehler beim Abrufen der Client-Liste: " + response.get("message"));
                return Collections.emptyList();
            }

            // Payload aus der Response
            Map<String,Object> p = (Map<String,Object>) response.get("payload");
            List<Map<String,Object>> clients = (List<Map<String,Object>>) p.get("entries");

            // Nach ID aufsteigend sortieren
            clients.sort(Comparator.comparingInt(c -> ((Number)c.get("id")).intValue()));

            return clients;

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Client-Liste: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private void listRobots() {
        try {
            Request<OperationRegistry, Entry.Type> request =
                    new Request<>(OperationRegistry.LIST, Entry.Type.ROBOT);

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                List<Map<String,Object>> entries = (List<Map<String,Object>>) p.get("entries");
                System.out.println("Bekannte Roboter:");
                for (Map<String,Object> e : entries) {
                    System.out.println("ID: "+  e.get("id")+", Name: "+e.get("name")+
                            ", IP: "+ e.get("ip")+", Port: "+  e.get("port"));
                }
            }

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Liste: " + e.getMessage());
        }
    }

    private void controlRobot() {
        System.out.print("Roboter Name eingeben: ");
        String robotName = scanner.nextLine();

        try {
            // Roboter-Liste abfragen
            Request<OperationRegistry, Entry.Type> request =
                    new Request<>(OperationRegistry.LIST, Entry.Type.ROBOT);
            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            Map<String,Object> robot = null;
            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                List<Map<String,Object>> entries = (List<Map<String,Object>>) p.get("entries");
                for (Map<String,Object> e : entries) {
                    if (robotName.equals(e.get("name"))) {
                        robot = e;
                        break;
                    }
                }
            }

            if (robot == null) {
                System.out.println("Roboter nicht gefunden!");
                return;
            }

            String robotIp = (String) robot.get("ip");
            int robotPort = ((Number) robot.get("port")).intValue();
            System.out.println("Verbinde zu Roboter " + robotName + " bei " + robotIp + ":" + robotPort);

            // Steuerbefehle senden (einfacher Request/Response)
            System.out.print("Befehl eingeben (z.B. MOVE 1 oder SHUTDOWN): ");

            Request<OperationRobot, Object> cmdRequest = readRobotCommand();

            if (request != null) {
                // Senden an RobotNode
                Map<String,Object> result = socketClient.sendRequest(robotIp, robotPort, cmdRequest);
                System.out.println("Antwort vom RobotNode: " + result);
            }

        } catch (Exception e) {
            System.err.println("Fehler bei Robotersteuerung: " + e.getMessage());
        }
    }

    /**
     * Liest einen Befehl vom Terminal ein und erstellt ein RobotRequest-Objekt
     * @return RobotRequest mit OperationRobot und optionaler Payload (MOVE-Prozentsatz)
     */
    private Request<OperationRobot, Object> readRobotCommand() {
        System.out.print("Befehl eingeben (z.B. MOVE 75 oder SHUTDOWN): ");
        String commandLine = scanner.nextLine().trim();

        if (commandLine.isEmpty()) {
            System.err.println("Kein Befehl eingegeben!");
            return null;
        }

        // Zerlegen in Teile
        String[] parts = commandLine.split("\\s+");

        // OperationRobot aus dem ersten Teil
        OperationRobot op;
        try {
            op = OperationRobot.valueOf(parts[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Ungültiger Befehl: " + parts[0]);
            return null;
        }

        // Payload vorbereiten
        Object payload = null;
        if (op == OperationRobot.MOVE) {
            int percentage = 100; // default
            if (parts.length > 1) {
                try {
                    percentage = Integer.parseInt(parts[1]);
                    percentage = Math.max(0, Math.min(100, percentage)); // Clamp auf 0–100
                } catch (NumberFormatException e) {
                    System.err.println("Ungültiger Prozentsatz, benutze 100%");
                }
            }
            payload = percentage;
        }

        // RobotRequest bauen
        return new Request<>(op, payload);
    }


    private void updateRingInfo() {
        while (true) {
            try {
                Thread.sleep(RING_UPDATE_INTERVAL_MS);
                //Map<String,Object> payload = Map.of("clientId", clientId);
                //Map<String,Object> request = Map.of("operation", "ringinfo", "payload", payload);

                Request<OperationRegistry, Integer> request =
                        new Request<>(OperationRegistry.RING_INFO, clientId);

                Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

                if ("ok".equals(response.get("status"))) {
                    Map<String,Object> p = (Map<String,Object>) response.get("payload");
                    prevId = ((Number)p.get("prev")).intValue();
                    nextId = ((Number)p.get("next")).intValue();
                    //System.out.println("Ring aktualisiert: prev=" + prevId + ", next=" + nextId);
                }

            } catch (Exception ignored) {
            }
        }
    }

    public void receiveToken(Map<String,Object> tokenMap) {
        String holder = (String) tokenMap.get("currentHolderId");
        System.out.println("Token empfangen von: " + holder);
        hasToken = true;

        // Roboter steuern
        controlRobotWithToken();

        // Token an Nachfolger senden
        sendToken();
    }

    private void sendToken() {
        if (!hasToken){
            return;
        }

        try {
            // Hole Nachfolger-IP/Port über Registry
            Map<String,Object> payload = Map.of("clientId", nextId);
            Map<String,Object> request = Map.of("operation", "getClientInfo", "payload", payload);

            //öffnet eune Soket-Verbindung zum Server
            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);
            if (!"ok".equals(response.get("status"))) {
                System.err.println("Fehler: Nachfolger nicht gefunden");
                return;
            }

            Map<String,Object> clientInfo = (Map<String,Object>) response.get("payload");
            String successorIp = (String) clientInfo.get("ip");
            int successorPort = ((Number) clientInfo.get("port")).intValue();

            // Token als Request senden
            Map<String,Object> tokenPayload = Map.of("currentHolderId", name);
            Map<String,Object> tokenRequest = Map.of("operation", "token", "payload", tokenPayload);

            socketClient.sendRequest(successorIp, successorPort, tokenRequest);
            System.out.println("Token an Nachfolger gesendet: ID=" + nextId);

            hasToken = false;

        } catch (Exception e) {
            System.err.println("Fehler beim Senden des Tokens: " + e.getMessage());
        }
    }

    private void controlRobotWithToken() {
        if (!hasToken) {
            System.out.println("Du hast kein Token. Kann den Roboter nicht steuern.");
            return;
        }

        System.out.println("Du hast das Token - Robotersteuerung aktiv!");
        controlRobot();
    }

    private void startTokenListener(int listenPort) {
        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(listenPort)) {
                while (true) {
                    Socket socket = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                    String json = in.readLine();
                    Map<String,Object> request = new Gson().fromJson(json, Map.class);

                    if ("token".equals(request.get("operation"))) {
                        Map<String,Object> payload = (Map<String,Object>) request.get("payload");
                        receiveToken(payload);
                        out.println("{\"status\":\"ok\"}");
                    } else {
                        out.println("{\"status\":\"unknown_operation\"}");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    public String getName() {
        return name;
    }
}
