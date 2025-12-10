package org.example;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import org.eclipse.paho.client.mqttv3.MqttException;

public class MasterCoordinator {

    private final int totalProcesses;
    private final MqttConnection brokerConn;
    private final Set<String> joinedClients;

    private final Map<String, Integer> endValues; // prozessId, M

    public MasterCoordinator(MqttConnection brokerConn, int totalProcesses) {
        this.brokerConn = brokerConn;
        this.totalProcesses = totalProcesses;

        joinedClients = ConcurrentHashMap.newKeySet();
        endValues = new ConcurrentHashMap<>();
    }

    public void start() throws Exception {
        subscribeInit();
        subscribeResult();
    }

    private void subscribeResult() throws Exception {
        brokerConn.subscribe("result", (topic, msg) -> {
            Gson gson = new Gson();
            String payload = new String(msg.getPayload());
            ValueMessage message = gson.fromJson(payload, ValueMessage.class);

            endValues.put(message.getSenderId(), message.getValue());
            if (endValues.size() == totalProcesses) {
                checkResult(message.getValue());
            }
        });
    }


    //wird am Ende aufgerufen
    private void checkResult(int endM) throws MqttException {
        System.out.println(endValues);
        for (Integer i : endValues.values()) {
            if (i != endM)
            {
                System.err.println("Ungleicher Endwert!!!");
                return;
            }
        }
        System.out.println("Endwert: " + endM);

        new Thread(() -> {
            try {
                brokerConn.disconnectAndClose();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();

    }

    private void subscribeInit() throws Exception {
        // Subscriber für init-Nachrichten
        brokerConn.subscribe("ring/init", (topic, msg) -> {
            Gson gson = new Gson();
            String payload = new String(msg.getPayload());
            ValueMessage message = gson.fromJson(payload, ValueMessage.class);
            joinedClients.add(message.getSenderId());
            if (joinedClients.size() == totalProcesses) {
                allClientsConnected();
            }
            System.out.println("[MASTER] INIT erhalten: " + message.getSenderId());
        });
    }


    private void allClientsConnected() {
        System.out.println("[MASTER] Alle Clients registriert, sende START-Nachricht!");

        // START-Nachricht an alle Clients
        ValueMessage startMsg = new ValueMessage(ValueMessage.Type.START, "master", 0);
        Gson gson = new Gson();
        String json = gson.toJson(startMsg);
        brokerConn.publish("start", json);
    }
}
