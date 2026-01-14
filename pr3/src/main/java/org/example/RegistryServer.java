package org.example;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.example.communication.OperationClient;
import org.example.communication.OperationRegistry;
import org.example.communication.Request;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.example.communication.OperationClient.TOKEN;


public class RegistryServer implements Runnable{

    //public static final String REGISTRY_HOST = "127.0.0.1";
    public static final String REGISTRY_HOST = "10.51.154.202";

    public static final int REGISTRY_PORT = 9000;

    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final Gson gson = new Gson();

    @Override
    public void run() {
        try {
            start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void start() throws Exception {
        System.out.println("Registry-Server gestartet auf Port " + REGISTRY_PORT);

        try (ServerSocket serverSocket = new ServerSocket(REGISTRY_PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                // Für jede Verbindung ein eigener Thread
                new Thread(() -> handle(clientSocket)).start();
            }
        }
    }

    /**
     * Verarbeitet eine eingehende Verbindung.
     * Liest genau eine JSON-Nachricht → beantwortet diese → beendet Verbindung.
     */
    private void handle(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line = in.readLine();
            //System.out.println(line);
            if (line == null) return;

            // 1. Request als POJO parsen

            // Typ definieren
            Type requestType = new TypeToken<Request<OperationRegistry, Object>>(){}.getType();

            Request<OperationRegistry, Object> request = gson.fromJson(line, requestType);

            OperationRegistry op = request.getOperation();
            //System.out.println(op);

            Map<String, Object> response;

            // 2. Operation auswählen
            switch (op) {
                case REGISTER -> {
                    Entry entry = gson.fromJson(
                            gson.toJson(request.getPayload()),
                            Entry.class
                    );
                    response = handleRegister(entry);
                }

                case UNREGISTER -> {
                    String name = gson.fromJson(
                            gson.toJson(request.getPayload()),
                            String.class
                    );
                    response = handleUnregister(name);
                }

                case LIST -> {
                    String typeStr = gson.fromJson(
                            gson.toJson(request.getPayload()),
                            String.class
                    );
                    Entry.Type type = Entry.Type.valueOf(typeStr);

                    response = handleList(type);
                }

                case RING_INFO -> {
                    Integer clientId = gson.fromJson(
                            gson.toJson(request.getPayload()),
                            Integer.class
                    );
                    response = handleRingInfo(clientId);
                }

                case CLIENT_INFO -> {
                    Integer clientId = gson.fromJson(
                            gson.toJson(request.getPayload()),
                            Integer.class
                    );

                    response = handleClientInfo(clientId);
                }

                default -> response = error("Unbekannte Operation: " + op);
            }

            out.println(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //soll zu einer ClientId den Port und Ip zurückgeben
    private Map<String, Object> handleClientInfo(int clientId) {

        for (Entry entry : entries.values()) {
            if (entry.getId() == clientId) {


                // Payload für Antwort
                Map<String, Object> p = new HashMap<>();
                p.put("ip", entry.getIp());
                p.put("port", entry.getPort());

                return ok(p);
            }
        }

        return error("Client mit ID " + clientId + " nicht gefunden");
    }


    /**
     * Verarbeitung der Operation: register.
     * Fügt einen Roboter/Client in die Registry ein.
     * Bedingungen:
     *  - Name muss eindeutig sein
     *  - ID wird automatisch vergeben
     */
    private Map<String, Object> handleRegister(Entry entry) {

        String name = entry.getName();

        if (entries.containsKey(name)) {
            return error("Name existiert bereits: " + name);
        }

        // ID automatisch vergeben
        int id = idCounter.getAndIncrement();

        Entry.Type type = entry.getType();
        String robotName = entry.getName();

        Entry newEntry = new Entry(
                id,
                robotName,
                entry.getIp(),
                entry.getPort(),
                type
        );
        entries.put(name, newEntry);
        if (Config.DEBUG)
        {
            System.out.println("Client registriert: " + newEntry);
        }

        // Payload für Antwort
        Map<String, Object> p = new HashMap<>();
        p.put("id", id);

        if (type == Entry.Type.ROBOT) {
            handleRegisterRobot(robotName);
        }

        return ok(p);
    }

    /**
     * erster Client muss Token erhalten
     */
    private void handleRegisterRobot(String robotName) {
        // Token als Request senden
        Request<OperationClient, String> tokenRequest = new Request<>(TOKEN, robotName);


        SocketClient socketClient = new SocketClient();

        Optional<Entry> minClientEntry = entries.values().stream()
                .filter(e -> e.getType() == Entry.Type.CLIENT)
                .min(Comparator.comparingInt(Entry::getId));


        minClientEntry.ifPresent(entry -> {
            try {
                socketClient.sendRequest(entry.getIp(), entry.getPort(), tokenRequest);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

    }


    /**
     * Verarbeitung der Operation: unregister.
     * Entfernt einen Eintrag anhand des Namens.
     */
    private Map<String, Object> handleUnregister(String name) {

        if (!entries.containsKey(name)) {
            return error("Unbekannter Name: " + name);
        }

        entries.remove(name);

        if (Config.DEBUG)
        {
            System.out.println("Client unregistriert: " + name);
        }

        return ok(null);
    }


    /**
     * Verarbeitung der Operation: list
     * type = "ROBOT"  → alle Roboter
     * type = "CLIENT" → alle Clients
     */
    private Map<String, Object> handleList(Entry.Type type) {

        List<Entry> result = generateListToType(type);

        Map<String, Object> p = new HashMap<>();
        p.put("entries", result);

        return ok(p);
    }

    private List<Entry> generateListToType(Entry.Type type) {
        return entries.values().stream()
                .filter(e -> e.getType() == type)
                .toList();
    }


    /**
     * Verarbeitung der Operation: ringinfo
     *  1. Alle Clients nach ID sortieren
     *  2. Für die angefragte ID prev/next bestimmen
     */
    private Map<String, Object> handleRingInfo(int requesterId) {

        //Alle Clients holener
        List<Entry> clients = entries.values()
                .stream()
                .filter(e -> e.getType() == Entry.Type.CLIENT)
                .sorted(Comparator.comparingInt(Entry::getId))
                .toList();

        if (clients.isEmpty()) {
            return error("Keine Clients registriert");
        }

        // Index der gesuchten ID finden
        int index = -1;
        for (int i = 0; i < clients.size(); i++) {
            if (clients.get(i).getId() == requesterId) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            return error("Client-ID nicht vorhanden");
        }

        Entry prev = clients.get((index - 1 + clients.size()) % clients.size());
        Entry next = clients.get((index + 1) % clients.size());

        Map<String, Object> p = new HashMap<>();
        p.put("prev", prev.getId());
        p.put("next", next.getId());

        return ok(p);
    }

    /**
     * Status ok und error
     */

    private Map<String, Object> ok(Object payload) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "ok");
        m.put("payload", payload);
        return m;
    }

    private Map<String, Object> error(String msg) {
        Map<String, Object> m = new HashMap<>();
        m.put("status", "error");
        m.put("message", msg);
        return m;
    }
}
