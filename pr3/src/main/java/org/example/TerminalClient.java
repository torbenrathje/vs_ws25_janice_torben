package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.communication.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.example.RegistryServer.REGISTRY_HOST;
import static org.example.RegistryServer.REGISTRY_PORT;
import static org.example.communication.OperationClient.TOKEN;

public class TerminalClient implements Runnable {

    private static final int RING_UPDATE_INTERVAL_MS = 5000;

    private final String name;
    private final String ip;
    private final int port;
    private int clientId;
    private int prevId, nextId;
    //private AtomicBoolean hasToken;
    private final Map<String, AtomicBoolean> robotTokens;

    private final Scanner scanner = new Scanner(System.in);
    private final SocketClient socketClient = new SocketClient();

    private Thread tokenListenerThread;
    private Thread ringUpdateThread;
    private volatile boolean running = true; // Mit volatile: Thread B sieht sofort, dass running auf false gesetzt wurde.
    private ServerSocket tokenServerSocket;

    private Map<String, PendingRobotCommand> pendingCommands;


    public TerminalClient(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        robotTokens = new ConcurrentHashMap<>();
        pendingCommands = new ConcurrentHashMap<>();
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
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("JVM Shutdown Hook ausgelöst");
            shutdown();
        }));

        //Registrierung beim Registry-Server
        if (!register()) {
            System.err.println("Registrierung fehlgeschlagen, beende.");
            return;
        }

        //Token-Listener starten (um eingehende Token-Requests zu empfangen)
        startTokenListener(port);

        //Ring-Update starten
        startRingUpdater();

        // 4. Falls erster Client: Token starten
        handleIfFirstClient();


        while (true) {
            System.out.println("\n--- Terminal Client ---");
            System.out.println("1: Liste Roboter anzeigen");
            System.out.println("2: Roboter auswaehlen und steuern");
            System.out.println("3: Topologie anzeigen");
            System.out.println("0: Beenden");
            System.out.print("Eingabe: ");
            String input = scanner.nextLine();

            switch (input) {
                case "1" -> listRobots();
                case "2" -> {
                    prepareRobotCommand();
                }
                case "3" -> {
                    topologie();
                }
                case "0" -> {
                    shutdown();
                    return;
                }
                default -> System.out.println("Ungueltige Eingabe");
            }
        }
    }


    // wenn erster Client: setze in der Map die Token und starte eventuelles weiterleiten des Tokens
    private void handleIfFirstClient() {
        List<Map<String,Object>> clients = listClients();

        if (!clients.isEmpty()) {
            // ID als Number auslesen und in int umwandeln
            Number firstIdNumber = (Number) clients.get(0).get("id");
            int firstClientId = firstIdNumber.intValue();

            if (clientId == firstClientId) {
                System.out.println("Ich bin der erste Client : Token erzeugt");

                List<Map<String,Object>> entries = getListOfRobots();
                if (entries != null) {
                    for (Map<String,Object> e : entries) {
                        String robotName = (String) e.get("name");
                        robotTokens.put(robotName, new AtomicBoolean(true)); // Token initial setzen
                        maybeForwardToken(robotName);
                    }
                }
            }
        }
    }

    private void topologie(){
        try {

            //Map<String,Object> payload = Map.of("clientId", clientId);
            //Map<String,Object> request = Map.of("operation", "ringinfo", "payload", payload);

            Request<OperationRegistry, Integer> request =
                    new Request<>(OperationRegistry.RING_INFO, clientId);

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                System.out.println("Vorgaenger: " + ((Number)p.get("prev")).intValue());
                System.out.println("Nachfolger: " + ((Number)p.get("next")).intValue());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void shutdown() {
        System.out.println("Shutdown eingeleitet");

        forwardAllTokensBeforeShutdown();

        unregister();//reihenfolge pruefen

        running = false;
        if (tokenListenerThread != null) {
            tokenListenerThread.interrupt(); // unterbricht eventuell accept
        }
        if (ringUpdateThread != null) {
            ringUpdateThread.interrupt();
        }

        try {
            if (tokenServerSocket != null) {
                tokenServerSocket.close();
            }
        } catch (Exception ignored) {}
    }

    private void startRingUpdater() {
        ringUpdateThread = new Thread(this::updateRingInfo, "UpdateInfo-Thread");
        ringUpdateThread.start();
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
        List<Map<String,Object>> entries = getListOfRobots();
        if (entries != null) {
            System.out.println("Bekannte Roboter:");
            for (Map<String,Object> e : entries) {
                int id = ((Number) e.get("id")).intValue();
                int port = ((Number) e.get("port")).intValue();

                System.out.println("ID: "+  id +", Name: "+e.get("name")+
                        ", IP: "+ e.get("ip")+", Port: "+  port);
            }
        }
    }

    //return: liste mit entries
    private List<Map<String,Object>> getListOfRobots() {
        try {
            Request<OperationRegistry, Entry.Type> request =
                    new Request<>(OperationRegistry.LIST, Entry.Type.ROBOT);

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                return (List<Map<String,Object>>) p.get("entries");
            }

        } catch (Exception e) {
            System.err.println("Fehler beim Abrufen der Liste: " + e.getMessage());
        }
        return null;
    }


    private void prepareRobotCommand() {
        System.out.print("Roboter Name eingeben: ");
        String robotName = scanner.nextLine();

        try {
            // Roboter suchen
            Request<OperationRegistry, Entry.Type> request =
                    new Request<>(OperationRegistry.LIST, Entry.Type.ROBOT);
            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            Map<String,Object> robot = null;
            Map<String,Object> p = (Map<String,Object>) response.get("payload");
            List<Map<String,Object>> entries = (List<Map<String,Object>>) p.get("entries");

            for (Map<String,Object> e : entries) {
                if (robotName.equals(e.get("name"))) {
                    robot = e;
                    break;
                }
            }

            if (robot == null) {
                System.out.println("Roboter nicht gefunden!");
                return;
            }

            Request<OperationRobot, Object> cmd = readRobotCommand();
            if (cmd == null) return;

            PendingRobotCommand prc = new PendingRobotCommand();
            prc.robotName = robotName;
            prc.robotIp = (String) robot.get("ip");
            prc.robotPort = ((Number) robot.get("port")).intValue();
            prc.request = cmd;

            pendingCommands.put(robotName, prc);

            System.out.println("Befehl vorbereitet. Warte auf Token...");

            AtomicBoolean token = robotTokens.get(robotName);
            if (token != null && token.get()) {
                sendPreparedCommand(prc);
            }

        } catch (Exception e) {
            System.err.println("Fehler: " + e.getMessage());
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
                    percentage = Math.max(0, Math.min(100, percentage)); // 0–100
                } catch (NumberFormatException e) {
                    System.err.println("Ungültiger Prozentsatz, benutze 100%");
                }
            }
            payload = percentage;
        }

        // RobotRequest bauen
        return new Request<>(op, payload);
    }

    // erfährt nextId und prevId im Ring periodisch vom Registry Server
    private void updateRingInfo() {
        while (running) {
            try {

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

                Thread.sleep(RING_UPDATE_INTERVAL_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break; // Thread sauber beenden

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Update Ring Info beendet");
    }

    public void receiveRobotToken(String robotName) {
        AtomicBoolean token = robotTokens.computeIfAbsent(robotName, k -> new AtomicBoolean(false));
        token.set(true);

        PendingRobotCommand cmd = pendingCommands.get(robotName);
        if (cmd != null) {
            sendPreparedCommand(cmd);
        } else {
            maybeForwardToken(robotName);
        }
    }

    private void sendPreparedCommand(PendingRobotCommand cmd) {
        try {
            Map<String,Object> result =
                    socketClient.sendRequest(cmd.robotIp, cmd.robotPort, cmd.request);

            System.out.println("Antwort vom RobotNode: " + result);

        } catch (Exception e) {
            System.err.println("Senden fehlgeschlagen: " + e.getMessage());
        } finally {
            pendingCommands.remove(cmd.robotName);
            robotTokens.get(cmd.robotName).set(false);
            sendToken(cmd.robotName);
        }
    }

    private void maybeForwardToken(String robotName) {
        new Thread(() -> {
            try {
                Thread.sleep(1000); // 1 Sekunde warten
                AtomicBoolean token = robotTokens.get(robotName);
                if (token != null && token.get()) {
                    sendToken(robotName);
                }
            } catch (InterruptedException ignored) {}
        }).start();
    }





    private void sendToken(String robotName) {
        AtomicBoolean token = robotTokens.get(robotName);
        if (!token.get()) {
            return;
        }

        try {

            //ids fangen bei 1 an
            //TODO next Id muss schon gesetzt sein
            if (nextId < 1) {
                //System.out.println("Kein Nachfolger gefunden");
                return;
            }

            Request<OperationRegistry, Integer> request = new Request<>(OperationRegistry.CLIENT_INFO, nextId);

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
            Request<OperationClient, String> tokenRequest = new Request<>(TOKEN, robotName);

            //hasToken.set(false); //TODO hier doch?
            socketClient.sendRequest(successorIp, successorPort, tokenRequest);//TODO wenn der gar nicht existiert hat keiner das Token
            //System.out.println("Token an Nachfolger gesendet: ID=" + nextId);

        } catch (Exception e) {
            System.err.println("Fehler beim Senden des Tokens: " + e.getMessage());
        }
    }



    // um Token zu erhalten
    private void startTokenListener(int listenPort) {
        tokenListenerThread = new Thread(() -> {
            try {
                tokenServerSocket = new ServerSocket(listenPort);

                while (running) {
                    try {
                        Socket socket = tokenServerSocket.accept();

                        BufferedReader in = new BufferedReader(
                                new InputStreamReader(socket.getInputStream()));
                        PrintWriter out = new PrintWriter(
                                socket.getOutputStream(), true);

                        String line = in.readLine();

                        Gson gson = new Gson();

                        //System.out.println("Input erhalten: " + line);
                        Type requestType = new TypeToken<Request<OperationClient, Object>>(){}.getType();
                        Request<OperationClient, ?> request = gson.fromJson(line, requestType);

                        Response<?> response;

                        if (request.getOperation() == TOKEN) {

                            String robotName = (String) request.getPayload();

                            receiveRobotToken(robotName);  // Token für diesen Roboter setzen
                            response = new Response<>(Response.Status.OK, null, "Token gesetzt");
                        } else {
                            response = new Response<>(Response.Status.ERROR, null, "Unbekannte Operation");
                        }
                        out.println(gson.toJson(response));

                    } catch (java.net.SocketException e) {
                        // Wird geworfen, wenn ServerSocket geschlossen wird
                        if (!running) {
                            System.out.println("TokenListener beendet");
                            break;
                        }
                        e.printStackTrace();
                    } catch (Exception e) {
                        if (running) e.printStackTrace();
                    }
                }

            } catch (Exception e) {
                if (running) e.printStackTrace();
            }
        }, "TokenListener-Thread");

        tokenListenerThread.start();
    }


    //wenn Tokens beim shutdown gehalten werden, müssen sie weitergegeben werden
    private void forwardAllTokensBeforeShutdown() {
        for (Map.Entry<String, AtomicBoolean> entry : robotTokens.entrySet()) {
            String robotName = entry.getKey();
            AtomicBoolean hasToken = entry.getValue();

            if (hasToken.get()) {
                System.out.println("Shutdown: Reiche Token für Roboter "
                        + robotName + " weiter");
                sendToken(robotName);
                hasToken.set(false);
            }
        }
    }




    public String getName() {
        return name;
    }


    class PendingRobotCommand {
        String robotName;
        String robotIp;
        int robotPort;
        Request<OperationRobot, Object> request;
    }
}
