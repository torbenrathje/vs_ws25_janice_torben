package org.example;

import org.eclipse.paho.client.mqttv3.*;

public class MqttConnection {
    private final IMqttAsyncClient client;

    public MqttConnection(String broker, String clientId) throws MqttException {
        System.out.println("[MQTT] Starte Async Client '" + clientId + "' auf Broker " + broker);
        client = new MqttAsyncClient(broker, clientId);

        //MqttConnectOptions options = new MqttConnectOptions();
        //options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1); // MQTT 5 aktivieren



        client.connect().waitForCompletion(); // Initial synchron, um Verbindung aufzubauen
        System.out.println("[MQTT] Client '" + clientId + "' verbunden.");
    }

    public void subscribe(String topic, IMqttMessageListener listener) throws MqttException {
        System.out.println("[MQTT] Subscribing '" + client.getClientId() + "' to topic: " + topic);
        client.subscribe(topic, 1, listener); // QoS 1, Listener direkt
    }

    // Asynchrones Publish
    public void publish(String topic, String json) {
        try {
            MqttMessage message = new MqttMessage(json.getBytes());
            message.setQos(1); // MQTT stufe, QoS Stufe 1 = At least once, Nachricht wird mindestens 1 mal geliefert, kann häufiger wenn ACK verloren geht

            client.publish(topic, message, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncToken) {
                    System.out.println("[MQTT ASYNC] Message sent to " + topic);
                }

                @Override
                public void onFailure(IMqttToken asyncToken, Throwable exception) {
                    System.out.println("[MQTT ASYNC ERROR] Failed to send message to " + topic);
                    exception.printStackTrace();
                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void disconnect() throws MqttException {
        System.out.println("[MQTT] Disconnecting client '" + client.getClientId() + "'");
        client.disconnect().waitForCompletion();
    }
}
