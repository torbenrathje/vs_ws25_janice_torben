package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.cads.vs.roboticArm.hal.simulation.CaDSRoboticArmSimulation;
import org.cads.vs.roboticArm.hal.real.CaDSRoboticArmReal;
import org.cads.vs.roboticArm.hal.ICaDSRoboticArm;
import org.example.communication.OperationRegistry;
import org.example.communication.OperationRobot;
import org.example.communication.Request;
import org.example.communication.Response;

import java.io.*;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import static org.example.RegistryServer.REGISTRY_HOST;
import static org.example.RegistryServer.REGISTRY_PORT;

/**
 * Einfacher Robot-Node
 * - registriert sich beim Registry-Server
 * - hält den Eintrag aktiv
 * - steuert den Roboter (Simulation oder real)
 */
public class RobotNode {
    private static final int UPDATE_INTERVAL_MS = 5000;

    private static final int MOVEMENT_PAUSE_MS = 1000;
    private static final int MOVEMENT_PAUSE_END = 3000;

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


    public void start() {
        try {
            // Roboter initialisieren
            if (useSimulation) {
                roboticArm = new CaDSRoboticArmSimulation();
            } else {
                roboticArm = new CaDSRoboticArmReal(ip, port);//TODO sollte das der gleiche IP + Port sein, wie Commands empfangen?
            }

            // Registry-Server registrieren
            if (!register()) {
                System.err.println("Registrierung fehlgeschlagen. Beende.");
                return;
            }

            // KeepAlive
            new Thread(this::keepAlive).start();

            // Command-Listener starten
            listenForCommands(port);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void listenForCommands(int port) {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(port)) {
                System.out.println("RobotNode " + name + " wartet auf Commands...");
                while (true) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                }
            } catch (IOException e) {
                System.err.println("Fehler beim Command-Listener: " + e.getMessage());
            }
        }, name + "-CommandListener").start();
    }

    private void handleClient(Socket client) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
             PrintWriter out = new PrintWriter(client.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null) return;

            Gson gson = new Gson();

            Type requestType = new TypeToken<Request<OperationRobot, Object>>(){}.getType();
            Request<OperationRobot, ?> request = gson.fromJson(line, requestType);

            Response<?> response;

            switch (request.getOperation()) {
                case MOVE -> {
                    Number number = (Number) request.getPayload();
                    int percentage = number.intValue();

                    simulateMovement(percentage);

                    response = new Response<>(Response.Status.OK, null, "MOVE ausgeführt");
                }

                case SHUTDOWN -> {
                    unregister();
                    shutdown();

                    response = new Response<>(Response.Status.OK, null, "Robot heruntergefahren");
                }

                default -> {
                    response = new Response<>(Response.Status.OK, null, "Unbekannte Operation");
                }
            }

            out.println(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }




    private void shutdown() {
        roboticArm.teardown();
    }

    private boolean register() {
        try (Socket socket = new Socket(REGISTRY_HOST, REGISTRY_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            Entry entry = new Entry(
                    -1,              // ID wird vom Server vergeben
                    name,
                    ip,
                    port,
                    Entry.Type.ROBOT
            );

            Request<OperationRegistry, Entry> req =
                    new Request<>(OperationRegistry.REGISTER, entry);

            out.println(gson.toJson(req));

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

            Request<OperationRegistry, String> request =
                    new Request<>(OperationRegistry.UNREGISTER, name);

            out.println(gson.toJson(request));

            System.out.println("Unregistrierung gesendet.");

        } catch (IOException e) {
            System.err.println("Registry nicht erreichbar beim Unregister: " + e.getMessage());
        }
    }

    //TODO braucht man das?
    private void keepAlive() {
        while (true) {
            try {
                Thread.sleep(UPDATE_INTERVAL_MS);
                //System.out.println("KeepAlive: " + name);
                // Hier könnte man optional nochmal Status senden
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // Movement Prozent in alle Richtungen setzen
    private void simulateMovement(int percentage) throws InterruptedException {
        percentage = Math.max(0, Math.min(percentage, 100)); //0 bis 100

        roboticArm.setBackForthPercentageTo(percentage);
        Thread.sleep(MOVEMENT_PAUSE_MS);

        roboticArm.setOpenClosePercentageTo(percentage);
        Thread.sleep(MOVEMENT_PAUSE_MS);

        roboticArm.setLeftRightPercentageTo(percentage);
        Thread.sleep(MOVEMENT_PAUSE_MS);

        roboticArm.setUpDownPercentageTo(percentage);
        Thread.sleep(MOVEMENT_PAUSE_END);
    }
}