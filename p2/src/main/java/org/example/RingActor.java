package org.example;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import com.google.gson.Gson;

public class RingActor extends AbstractActor {

    private int id;
    private int M;
    private ActorRef prev;
    private ActorRef next;
    private Gson gson = new Gson();

    public RingActor(int id, int initialM, ActorRef prev, ActorRef next) {
        this.id = id;
        this.M = initialM;
        this.prev = prev;
        this.next = next;
    }

    public static class SetNext {
        public final ActorRef next;
        public SetNext(ActorRef next) { this.next = next; }
    }

    public static class SetPrev {
        public final ActorRef prev;
        public SetPrev(ActorRef prev) { this.prev = prev; }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(String.class, jsonStr -> {
                    Message msg = gson.fromJson(jsonStr, Message.class);
                    int y = msg.getValue();

                    if (y < M) {
                        M = (M - 1) % y + 1;

                        Message outMsg = new Message(Message.Type.UPDATE, id, M);
                        String outJson = gson.toJson(outMsg);

                        if (prev != null) prev.tell(outJson, getSelf());
                        if (next != null) next.tell(outJson, getSelf());
                    }
                })
                .match(SetNext.class, s -> this.next = s.next)
                .match(SetPrev.class, s -> this.prev = s.prev)
                .build();
    }

    public int getId() {
        return id;
    }

    public int getM() {
        return M;
    }
}
