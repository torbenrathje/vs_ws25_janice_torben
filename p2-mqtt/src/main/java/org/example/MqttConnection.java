package org.example;

import org.eclipse.paho.client.mqttv3.*;

public class MqttConnection {
    private final IMqttClient client;

    public MqttConnection(String broker, String clientId) throws Exception {
        System.out.println("[MQTT] Starte Client '" + clientId + "' auf Broker " + broker);
        client = new MqttClient(broker, clientId);

        client.connect();
        System.out.println("[MQTT] Client '" + clientId + "' verbunden.");
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws Exception {
        System.out.println("[MQTT] Subscribing '" + client.getClientId() + "' to topic: " + topic);
        client.subscribe(topic, (t, msg) -> {
            String payload = new String(msg.getPayload());
            System.out.println("[MQTT] <== Received on " + t + " by " + client.getClientId() + ": " + payload);
            listener.messageArrived(t, msg); // deine Logik weiterreichen
        });
    }

    public void publish(String topic, String json) throws Exception {
        System.out.println("[MQTT] ==> Publishing from " + client.getClientId() +
                " to " + topic + ": " + json);

        client.publish(topic, json.getBytes(), 0, false);
    }

    public void disconnect() throws Exception {
        System.out.println("[MQTT] Disconnecting client '" + client.getClientId() + "'");
        client.disconnect();
    }
}
