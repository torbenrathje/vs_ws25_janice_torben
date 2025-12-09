package org.example;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class RegistryServer {
   private  Map<String, Entry> registry = new ConcurrentHashMap<>();
   private  int idCounter;
   private int port;

   public RegistryServer(int port) {
        idCounter = 0;
        this.port = port;
   }

   private void register(String name, String ip, int port) {
       if(registry.containsKey(name)){
           //fehler zurückgeben
       }

       int id = idCounter++;
       //registry.put(name, new Entry(id, name, ip, port, ));
   }


}
