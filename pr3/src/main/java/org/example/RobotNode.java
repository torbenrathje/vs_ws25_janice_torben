package org.example;

import com.google.gson.Gson;
import org.cads.vs.roboticArm.hal.simulation.CaDSRoboticArmSimulation;
import org.cads.vs.roboticArm.hal.real.CaDSRoboticArmReal;
import org.cads.vs.roboticArm.hal.ICaDSRoboticArm;

import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Einfacher Robot-Node
 * - registriert sich beim Registry-Server
 * - hält den Eintrag aktiv
 * - steuert den Roboter (Simulation oder real)
 */
public class RobotNode {

    private static final String REGISTRY_HOST = "127.0.0.1";
    private static final int REGISTRY_PORT = 9000;
    private static final int UPDATE_INTERVAL_MS = 5000;

    private final String name;
    private final String ip;
    private final int port;
    private final boolean useSimulation;
    private ICaDSRoboticArm roboticArm;

    private final Gson gson = new Gson();
    private int clientId;

    public RobotNode(String name, String ip, int port, boolean useSimulation) {
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.useSimulation = useSimulation;
    }

    public static void main(String[] args) throws Exception {
        // Beispiel: RobotNode node = new RobotNode("robot1", "127.0.0.1", 10001, true);
        RobotNode node = new RobotNode("robot1", "127.0.0.1", 10001, true);
        node.start();
    }

    public void start() {
        try {
            // Roboter initialisieren
            if (useSimulation) {
                roboticArm = new CaDSRoboticArmSimulation();
            } else {
                roboticArm = new CaDSRoboticArmReal(ip, port);
            }

            // Registry-Server registrieren
            if (!register()) {
                System.err.println("Registrierung fehlgeschlagen. Beende.");
                return;
            }

            // Periodisches Update des Eintrags
            new Thread(this::keepAlive).start();

            // Einfacher Test: Roboterbewegung simulieren
            simulateMovement();

            // Beenden -> unregister
            Runtime.getRuntime().addShutdownHook(new Thread(this::unregister));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean register() {
        try (Socket socket = new Socket(REGISTRY_HOST, REGISTRY_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Map<String,Object> payload = new HashMap<>();
            payload.put("name", name);
            payload.put("ip", ip);
            payload.put("port", port);
            payload.put("type", "ROBOT");

            Map<String,Object> request = new HashMap<>();
            request.put("operation", "register");
            request.put("payload", payload);

            out.println(gson.toJson(request));

            String responseLine = in.readLine();
            Map response = gson.fromJson(responseLine, Map.class);
            if ("ok".equals(response.get("status"))) {
                Map<String,Object> p = (Map<String,Object>) response.get("payload");
                clientId = ((Number) p.get("id")).intValue();
                System.out.println("Registrierung erfolgreich: ID=" + clientId);
                return true;
            } else {
                System.err.println("Registrierung fehlgeschlagen: " + response.get("message"));
                return false;
            }

        } catch (IOException e) {
            System.err.println("Registry nicht erreichbar: " + e.getMessage());
            return false;
        }
    }

    private void unregister() {
        try (Socket socket = new Socket(REGISTRY_HOST, REGISTRY_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Map<String,Object> payload = new HashMap<>();
            payload.put("name", name);

            Map<String,Object> request = new HashMap<>();
            request.put("operation", "unregister");
            request.put("payload", payload);

            out.println(gson.toJson(request));

            System.out.println("Unregistrierung gesendet.");

        } catch (IOException e) {
            System.err.println("Registry nicht erreichbar beim Unregister: " + e.getMessage());
        }
    }

    private void keepAlive() {
        while (true) {
            try {
                Thread.sleep(UPDATE_INTERVAL_MS);
                System.out.println("KeepAlive: " + name);
                // Hier könnte man optional nochmal Status senden
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void simulateMovement() throws InterruptedException {
        // Beispiel: einfache Schleife für Simulation
        System.out.println("Starte Roboter-Simulation");
        for (int i=0;i<5;i++) {
            System.out.println("Bewege Roboter auf Position " + i);
            Thread.sleep(2000);
        }
        System.out.println("Simulation beendet.");
    }
}