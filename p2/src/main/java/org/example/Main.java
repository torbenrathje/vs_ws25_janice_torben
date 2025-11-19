package org.example;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;

import java.util.ArrayList;
import java.util.List;

public class Main {

    /**
     * Prozesse in array erzeugen (new RingActors)
     * actor referenzen darauf erstellen
     * dann prev und next für alle setzen
     * startnachrichten senden
     * paar sekunden warten
     * test ob alle den gleichen endwert haben
     * @param args
     * @throws InterruptedException
     */

    /**
     * 2, 2, 5, [3 ,6, 9, 89, 7] startargumente
     * ; ANzhal gesamt rechner; "rechner ID", anzahl an prozessen; liste mit zahlen,( dynamisch)
     *
     * liste durchgehen in schleife; rechner 1 erster wert, rechner 2 zweiter wert, von vorne bis ende erreicht
     *
     *
     * @param args
     * @throws InterruptedException
     */

    public static void main(String[] args) throws InterruptedException {

        // Beispiel: 5 Prozesse, 2 Rechner (hier nur Simulation auf 1 JVM)
        ActorSystem system = ActorSystem.create("RingSystem");
        for (String arg : args)
        {
            System.out.println(arg);
        }

        int numPc;
        int pcId;
        int numProcesses;
        List<Integer> numbers = new ArrayList<>();
        numPc = Integer.parseInt(args[0]);
        pcId = Integer.parseInt(args[1]);
        numProcesses = Integer.parseInt(args[2]);

        for (int i = 3; i < numProcesses+3; i++)
        {
            numbers.add(Integer.parseInt(args[i]));
        }




        List<RingActor> ringActors = new ArrayList<>();
        for (int i = 0; i <numProcesses; i++)
        {
            ringActors.add(new RingActor(i, numbers.get(i), null, null));
        }

        // ACTOR REFERENZEN
        List<ActorRef> ringActorRef = new ArrayList<>();
        for (int i = 0; i < numProcesses; i++) {
            final int index = i; // Lambda braucht effectively final
            String actorName = "p" + i; // eindeutiger Name

            // Actor erstellen
            ActorRef actor = system.actorOf(
                    Props.create(RingActor.class, () -> ringActors.get(index)),
                    actorName
            );

            ringActorRef.add(actor);
        }


        // Ring verbinden
        for (int i = 0; i < ringActorRef.size(); i++) {
            ActorRef prev = ringActorRef.get((i - 1 + ringActorRef.size()) % ringActorRef.size());
            ActorRef next = ringActorRef.get((i + 1) % ringActorRef.size());


            ringActorRef.get(i).tell(new RingActor.SetPrev(prev), ActorRef.noSender()); // TODO
            ringActorRef.get(i).tell(new RingActor.SetNext(next), ActorRef.noSender()); // warum statik
        }

        //startnachrichten
        for (int i = 0; i <numProcesses; i++)
        {
            Message msg = new Message(Message.Type.INIT, ringActors.get(i).getId(), ringActors.get(i).getM()); // hier 0 oder initialer Wert
            String json = new com.google.gson.Gson().toJson(msg);
            ringActorRef.get(i).tell(json, ActorRef.noSender());
        }

        // Wartezeit, um den Algorithmus laufen zu lassen
        Thread.sleep(5000);

        system.terminate();
    }
}
