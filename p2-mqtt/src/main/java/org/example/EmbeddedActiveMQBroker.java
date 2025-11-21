package org.example;

import org.apache.activemq.broker.BrokerService;

public class EmbeddedActiveMQBroker {
    public static void main(String[] args) throws Exception {
        BrokerService broker = new BrokerService();
        broker.setBrokerName("EmbeddedBroker");
        broker.addConnector("tcp://0.0.0.0:61616");
        broker.setPersistent(false);

        broker.start();
        System.out.println("ActiveMQ Embedded Broker gestartet auf tcp://0.0.0.0:61616");

        // Den Broker am Leben halten
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                broker.stop();
                System.out.println("ActiveMQ Broker gestoppt");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));

        // Hauptthread blockieren
        Thread.currentThread().join(); // wartet unendlich
    }
}


