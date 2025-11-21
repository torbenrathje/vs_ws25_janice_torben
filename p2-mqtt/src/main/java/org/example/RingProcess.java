package org.example;

import com.google.gson.Gson;

public class RingProcess {
    private final String id;
    private String prev;
    private String next;

    private int M;
    private final MqttConnection conn;
    private final Gson gson = new Gson();

    //private final Object lock = new Object();


    public RingProcess(String id, int initialValue, String prev, String next, MqttConnection conn) {
        this.id = id;
        this.M = initialValue;
        this.prev = prev;
        this.next = next;
        this.conn = conn;

        System.out.println("[INIT] Process " + id + " started with M=" + initialValue +
                ", prev=" + prev + ", next=" + next);

        subscribeToOwnTopic();
        subscribeToStartTopic();

        publishInit();
    }

    private void publishInit() {
        ValueMessage msg = new ValueMessage(ValueMessage.Type.INIT, id, 0);
        String json = gson.toJson(msg);
        conn.publish("ring/init", json);
    }

    private void subscribeToStartTopic() {
        String topic = "start";
        try {
            conn.subscribe(topic, (t, msg) -> {
                System.out.println("[RECEIVE] " + id + " received message on " + t + ": " );
                broadcast();
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void subscribeToOwnTopic() {
        // Subscribe to own topic
        String topic = "ring/" + id;
        System.out.println("[MQTT] " + id + " subscribing to topic: " + topic);
        try {
            conn.subscribe(topic, (t, msg) -> {
                String payload = new String(msg.getPayload());
                System.out.println("[RECEIVE] " + id + " received message on " + t + ": " + payload);

                ValueMessage m = gson.fromJson(payload, ValueMessage.class);
                onMessage(m);
            });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void onMessage(ValueMessage m) {
        //synchronized (lock) {
        int y = m.getValue();
        System.out.println("[PROCESS] " + id + " processing message with value y=" + y + ", current M=" + M);

        if (y < M) {
            int oldM = M;
            M = ((oldM - 1) % y) + 1;

            System.out.println("[UPDATE] " + id + ": M changed from " + oldM + " → " + M);

            try {
                broadcast();
            } catch (Exception e) {
                System.out.println("[ERROR] " + id + " failed to broadcast:");
                e.printStackTrace();
            }
        } else {
            System.out.println("[SKIP] " + id + ": y >= M, no update performed");
        }
        //}
    }

    private void broadcast() {
        ValueMessage msg = new ValueMessage(ValueMessage.Type.UPDATE, id, M);
        String json = gson.toJson(msg);

        System.out.println("[BROADCAST] " + id + " sending M=" + M +
                " to prev=" + prev + ", next=" + next);

        conn.publish("ring/" + prev, json);
        conn.publish("ring/" + next, json);
    }

    public int getM() {
        return M;
    }

    public void setPrev(String prev) {
        System.out.println("[SET] " + id + " prev changed to " + prev);
        this.prev = prev;
    }

    public void setNext(String next) {
        System.out.println("[SET] " + id + " next changed to " + next);
        this.next = next;
    }

    public String getId() {
        return id;
    }

    public String getPrev() {
        return prev;
    }

    public String getNext() {
        return next;
    }
}
