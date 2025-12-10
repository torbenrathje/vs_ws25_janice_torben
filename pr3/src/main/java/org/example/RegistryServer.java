package org.example;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Dieser Server stellt einen zentralen Namens- und Koordinationsdienst bereit.
 *
 * Verantwortlichkeiten:
 *  - Registrieren von Robotern und Clients
 *  - Verhindern doppelter Namen
 *  - Zuteilen eindeutiger IDs (wichtig für Token-Ring)
 *  - Auflisten von Robotern/Clients
 *  - Optional: Berechnung von prev/next im Token-Ring
 *
 * KEINE Aufgaben des Registry-Servers:
 *  - Weiterleitung von Roboternachrichten
 *  - Speicherung auf Festplatte
 *  - Token-Verteilung oder Token-Handling
 *
 * Kommunikationsprotokoll:
 *  - TCP, JSON-basierte Requests & Responses
 *
 * Nebenläufigkeit:
 *  - Thread-pro-Verbindung (einfachstes und für das Praktikum empfohlenes Modell)
 */
public class RegistryServer {
    private static final int PORT = 9000;

    /**
     * Speicher aller registrierten Einträge
     * Key = Name des Knotens (eindeutig)
     * Value = Entry-Objekt
     *
     * ConcurrentHashMap → thread-sicher für parallele Zugriffe
     * Persistenz → Daten werden auf dem RAM gespeichert
     */
    private final Map<String, Entry> entries = new ConcurrentHashMap<>();
    private final AtomicInteger idCounter = new AtomicInteger(1);
    private final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        new RegistryServer().start();
    }

    public void start() throws Exception {
        System.out.println("Registry-Server gestartet auf Port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
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
            if (line == null) return;

            Map<String, Object> request = gson.fromJson(line, Map.class);
            String op = (String) request.get("operation");
            Map<String, Object> payload = (Map<String, Object>) request.get("payload");

            Map<String, Object> response;

            //Operation auswählen
            switch (op) {
                case "register" -> response = handleRegister(payload);
                case "unregister" -> response = handleUnregister(payload);
                case "list" -> response = handleList(payload);
                case "ringinfo" -> response = handleRingInfo(payload);
                default -> response = error("Unbekannte Operation: " + op);
            }

            out.println(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Verarbeitung der Operation: register
     * -----------------------------------
     * Fügt einen Roboter/Client in die Registry ein.
     * Bedingungen:
     *  - Name muss eindeutig sein
     *  - ID wird automatisch vergeben
     */
    private Map<String, Object> handleRegister(Map<String, Object> payload) {

        String name = (String) payload.get("name");
        String ip = (String) payload.get("ip");
        // JSON speichert Zahlen als "double", daher konvertieren
        double portDouble = (double) payload.get("port");
        int port = (int) portDouble;
        String typeStr = (String) payload.get("type");

        Entry.Type type = Entry.Type.valueOf(typeStr);

        if (entries.containsKey(name)) {
            return error("Name existiert bereits: " + name);
        }

        int id = idCounter.getAndIncrement();
        Entry entry = new Entry(id, name, ip, port, type);
        entries.put(name, entry);

        // Payload für Antwort
        Map<String, Object> p = new HashMap<>();
        p.put("id", id);

        return ok(p);
    }

    /**
     * Verarbeitung der Operation: unregister
     * --------------------------------------
     * Entfernt einen Eintrag anhand des Namens.
     */
    private Map<String, Object> handleUnregister(Map<String, Object> payload) {
        String name = (String) payload.get("name");

        if (!entries.containsKey(name)) {
            return error("Unbekannter Name: " + name);
        }

        entries.remove(name);
        return ok(null);
    }

    /**
     * Verarbeitung der Operation: list
     * --------------------------------
     * Gibt alle Einträge eines Typs zurück.
     *
     * type = "ROBOT"  → alle Roboter
     * type = "CLIENT" → alle Clients
     */
    private Map<String, Object> handleList(Map<String, Object> payload) {
        String typeStr = (String) payload.get("type");
        Entry.Type type = Entry.Type.valueOf(typeStr);

        List<Map<String, Object>> result = new ArrayList<>();

        for (Entry e : entries.values()) {
            if (e.getType() == type) {
                Map<String, Object> row = new HashMap<>();
                row.put("id", e.getId());
                row.put("name", e.getName());
                row.put("ip", e.getIp());
                row.put("port", e.getPort());
                row.put("type", e.getType().toString());
                result.add(row);
            }
        }

        Map<String, Object> p = new HashMap<>();
        p.put("entries", result);

        return ok(p);
    }

    /**
     * Verarbeitung der Operation: ringinfo
     * ------------------------------------
     * OPTIONAL: Berechnet predecessor / successor eines Clients im Token-Ring.
     *
     *  1. Alle Clients nach ID sortieren
     *  2. Für die angefragte ID prev/next bestimmen
     */
    private Map<String, Object> handleRingInfo(Map<String, Object> payload) {

        int requesterId = ((Number) payload.get("clientId")).intValue();

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

/**
 * REGISTRY-SERVICE – ÜBERSICHT
 * ----------------------------
 * Der Registry-Service ist ein zentraler Server, der Informationen über
 * alle aktiven Clients und Roboterarme im System verwaltet.
 *
 * Ziel:
 *  - Roboter- und Client-Knoten können sich mit Name, IP und Port registrieren.
 *  - Ein Terminal-Client kann nach verfügbaren Robotern fragen.
 *  - Ein Terminal-Client bekommt eine eindeutige, fortlaufende ID für den Token Ring.
 *  - Registry verhindert doppelte Namen.
 *  - Registry verteilt keine Nachrichten zwischen Clients und Robotern, sondern
 *    dient nur als Namens- & Koordinationsdienst.
 *================================================================================
 * ANFORDERUNGEN UND SERVERVERHALTEN
 * ================================================================================
 *
 * 1) ANWENDUNGSPROTOKOLL (JSON)
 * Der Registry-Server kommuniziert ausschließlich über JSON-Nachrichten.
 * Jede Nachricht hat den Aufbau:
 *  {
 *   "operation": "register" | "unregister" | "list",
 *   "payload": { ... }
 *   }
 *
 *   2) GESPEICHERTE ATTRIBUTE PRO KNOTEN
 *      *      - int id         -> fortlaufende, eindeutige ID (für Token Ring)
 *      *      - String name    -> eindeutiger Name des Knotens
 *      *      - String ip      -> IP-Adresse des Knotens
 *      *      - int port       -> Port des Knotens
 *      *      - String type    -> "client" oder "robot"
 *
 * 3) OPERATIONEN
 * --------------------------------------------------------------------------------
 *    register(name, ip, port, type)
 *    -------------------------------
 *    - Ein Client oder Roboter meldet sich an.
 *    - Registry prüft, ob der Name bereits existiert.
 *         -> Wenn ja → Fehler zurückgeben.
 *    - Wenn der Name frei ist:
 *         -> neue ID generieren
 *         -> Eintrag in Speicher ablegen
 *         -> Bestätigung an Client senden
 *
 *    Beispiel Response:
 *      {
 *        "status": "ok",
 *        "id": 3,
 *        "message": "registered"
 *      }
 *
 *
 *    unregister(name)
 *    ----------------
 *    - Entfernt einen Eintrag aus der Registry.
 *    - Wenn Name nicht existiert → Fehler senden.
 *
 *
 *    list(type)
 *    ----------
 *    - Gibt alle Einträge des gewünschten Typs zurück:
 *      type = "robot" → alle Roboterarme
 *      type = "client" → alle Clients
 *      type = "all" → alles
 *
 *    Beispiel:
 *      {
 *        "status": "ok",
 *        "entries": [
 *          {"id":1, "name":"arm1", "ip":"127.0.0.1", "port":9000, "type":"robot"},
 *          ...
 *        ]
 *      }
 *
 *
 * ================================================================================
 * 4) NEBENLÄUFIGKEIT
 * ================================================================================
 * Der Server muss mehrere parallele Verbindungen unterstützen.
 *
 * GEEIGNETE STRATEGIEN (eine auswählen):
 *   ✔ Thread pro Verbindung (einfachste Lösung)
 *   ✔ Thread-Pool (performanter)
 *   ✔ NIO (komplexer, aber skalierbar)
 *
 * Empfehlung für Praktikum: THREAD-PRO-VERBINDUNG
 *   - pro Client-Connection wird ein eigener Thread erzeugt
 *   - einfacher zu implementieren + gut verständlich
 *
 *
 * ================================================================================
 * 5) FEHLERVERHALTEN
 * ================================================================================
 *  - doppelte Namen → {"status": "error", "message":"name already in use"}
 *  - ungültige Nachrichten → {"status":"error", "message":"invalid request"}
 *  - ungültiger Typ → {"status":"error", "message":"unknown type"}
 *
 *
 * ================================================================================
 * 6) PERSISTENZ
 * ================================================================================
 *  - Alles wird nur im RAM gespeichert (HashMap)
 *  - Keine Datenbank nötig
 *
 *
 * ================================================================================
 * 7) TOKEN-RING-LOGIK (für Terminal-Clients)
 * ================================================================================
 * Der Registry-Service vergibt fortlaufende IDs.
 * Die Clients bilden daraus eine logische Ring-Topologie:
 *
 *  - Jeder Client fragt regelmäßig:
 *        list("client")
 *
 *  - Daraus bestimmt der Client:
 *        Vorgänger-ID: größte ID kleiner als meine ID
 *        Nachfolger-ID: kleinste ID größer als meine ID
 *
 *  - Der Client mit der kleinsten ID erzeugt einmalig das Token.
 *
 * Registry selbst macht KEINE Token-Verwaltung!
 * Sie verteilt nur IDs.
 *
 *
 * ================================================================================
 * 8) START UND LAUFZEITVERHALTEN
 * ================================================================================
 *  - Server startet → hört auf einem TCP-Port
 *  - Für jede eingehende Verbindung:
 *        -> Thread wird gestartet
 *        -> JSON-Payload wird verarbeitet
 *        -> Antwort wird zurückgeschickt
 *  - Beim Beenden eines Knotens ruft dieser unregister(name) auf.
 *
 */
