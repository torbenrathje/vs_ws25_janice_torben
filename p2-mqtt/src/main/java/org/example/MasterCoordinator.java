package org.example;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class MasterCoordinator {

    private final int totalProcesses;
    private final MqttConnection brokerConn;
    private final Set<String> joinedClients = ConcurrentHashMap.newKeySet();

    public MasterCoordinator(MqttConnection brokerConn, int totalProcesses) {
        this.brokerConn = brokerConn;
        this.totalProcesses = totalProcesses;
    }

    public void start() throws Exception {
        // Subscriber für JOIN-Nachrichten
        brokerConn.subscribe("ring/init", (topic, msg) -> {
            Gson gson = new Gson();
            String payload = new String(msg.getPayload());
            ValueMessage message = gson.fromJson(payload, ValueMessage.class);
            joinedClients.add(message.getSenderId());
            System.out.println("[MASTER] INIT erhalten: " + message.getSenderId());
        });

        // Warten, bis alle Clients gejoined sind
        System.out.println("[MASTER] Warten auf alle Clients...");
        while (joinedClients.size() < totalProcesses) {
            Thread.sleep(50);
        }
        //TODO vllt direkt in subscribe join rein, damit nicht warten muss

        System.out.println("[MASTER] Alle Clients registriert, sende START-Nachricht!");

        // START-Nachricht an alle Clients
        ValueMessage startMsg = new ValueMessage(ValueMessage.Type.START, "master", 0);
        Gson gson = new Gson();
        String json = gson.toJson(startMsg);
        brokerConn.publish("start", json);
    }
}
