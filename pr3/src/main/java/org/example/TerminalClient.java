package org.example;

import java.util.*;

public class TerminalClient {

    private static final String REGISTRY_HOST = "127.0.0.1";
    private static final int REGISTRY_PORT = 9000;
    private static final int RING_UPDATE_INTERVAL_MS = 5000;

    private final String name;
    private final String ip;
    private final int port;
    private int clientId;
    private int prevId, nextId;

    private final Scanner scanner = new Scanner(System.in);
    private final SocketClient socketClient = new SocketClient();

    public TerminalClient(String name, String ip, int port) {
        this.name = name;
        this.ip = ip;
        this.port = port;
    }

    public static void main(String[] args) throws Exception {
        TerminalClient client = new TerminalClient("client1", "127.0.0.1", 10000);
        client.start();
    }

    public void start() throws Exception {
        if (!register()) {
            System.err.println("Registrierung fehlgeschlagen, beende.");
            return;
        }

        new Thread(this::updateRingInfo).start();

        while (true) {
            System.out.println("\n--- Terminal Client ---");
            System.out.println("1: Liste Roboter anzeigen");
            System.out.println("2: Roboter auswählen und steuern");
            System.out.println("0: Beenden");
            System.out.print("Eingabe: ");
            String input = scanner.nextLine();

            switch (input) {
                case "1" -> listRobots();
                case "2" -> controlRobot();
                case "0" -> {
                    unregister();
                    return;
                }
                default -> System.out.println("Ungültige Eingabe");
            }
        }
    }

    private boolean register() {
        try {
            Map<String,Object> payload = Map.of(
                    "name", name,
                    "ip", ip,
                    "port", port,
                    "type", "CLIENT"
            );

            Map<String,Object> request = Map.of(
                    "operation", "register",
                    "payload", payload
            );

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

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
            Map<String,Object> payload = Map.of("name", name);
            Map<String,Object> request = Map.of("operation", "unregister", "payload", payload);
            socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);
            System.out.println("Unregistrierung gesendet.");
        } catch (Exception e) {
            System.err.println("Fehler beim Unregister: " + e.getMessage());
        }
    }

    private void listRobots() {
        try {
            Map<String,Object> payload = Map.of("type", "ROBOT");
            Map<String,Object> request = Map.of("operation", "list", "payload", payload);

            Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                List<Map<String,Object>> entries = (List<Map<String,Object>>) p.get("entries");
                System.out.println("Bekannte Roboter:");
                for (Map<String,Object> e : entries) {
                    System.out.println("ID: "+e.get("id")+", Name: "+e.get("name")+
                            ", IP: "+e.get("ip")+", Port: "+e.get("port"));
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
            Map<String,Object> payload = Map.of("type", "ROBOT");
            Map<String,Object> request = Map.of("operation", "list", "payload", payload);
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
            System.out.print("Befehl eingeben (z.B. move 1): ");
            String command = scanner.nextLine();

            Map<String,Object> cmdPayload = Map.of("command", command);
            Map<String,Object> cmdRequest = Map.of("operation", "command", "payload", cmdPayload);

            Map<String,Object> result = socketClient.sendRequest(robotIp, robotPort, cmdRequest);
            System.out.println("Roboter antwortet: " + result.get("status"));

        } catch (Exception e) {
            System.err.println("Fehler bei Robotersteuerung: " + e.getMessage());
        }
    }

    private void updateRingInfo() {
        while (true) {
            try {
                Thread.sleep(RING_UPDATE_INTERVAL_MS);
                Map<String,Object> payload = Map.of("clientId", clientId);
                Map<String,Object> request = Map.of("operation", "ringinfo", "payload", payload);

                Map<String,Object> response = socketClient.sendRequest(REGISTRY_HOST, REGISTRY_PORT, request);

                if ("ok".equals(response.get("status"))) {
                    Map<String,Object> p = (Map<String,Object>) response.get("payload");
                    prevId = ((Number)p.get("prev")).intValue();
                    nextId = ((Number)p.get("next")).intValue();
                    System.out.println("Ring aktualisiert: prev=" + prevId + ", next=" + nextId);
                }

            } catch (Exception ignored) {
            }
        }
    }
}
