package org.example;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) throws Exception {

        String broker = "tcp://localhost:1883";
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


        MqttConnection[] connections = new MqttConnection[5];
        // Create MQTT clients
        for (int i = 0; i < 5; i++) {
            try {
                connections[i] = new MqttConnection(broker, "p" + i);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


        List<RingProcess> ringProcesses = new ArrayList<>();
        for (int i = 0; i <numProcesses; i++)
        {
            ringProcesses.add(new RingProcess("p"+i, numbers.get(i), null, null, connections[i]));
        }



        // Ring verbinden
        for (int i = 0; i < numProcesses; i++) {
            String prev = ringProcesses.get((i - 1 + numProcesses) % numProcesses).getId();
            String next = ringProcesses.get((i + 1) % numProcesses).getId();

            ringProcesses.get(i).setNext(next);
            ringProcesses.get(i).setPrev(prev);
        }
        System.out.println("test1");
        //startnachrichten
        //for (RingProcess proc : ringProcesses)
        //{
        //    proc.broadcast();
        //}
        ringProcesses.get(0).broadcast();

        System.out.println("test2");

        ringProcesses.get(1).broadcast();

        System.out.println("test2");

        ringProcesses.get(2).broadcast();

        System.out.println("test2");

        ringProcesses.get(3).broadcast();

        System.out.println("test2");

        // Wartezeit, um den Algorithmus laufen zu lassen
        Thread.sleep(2000);


        for (RingProcess proc : ringProcesses)
        {
            System.out.println(proc.getM());
        }

        // disconnect
        for (int i = 0; i < numProcesses; i++) {
            connections[i].disconnect();
        }
    }
}
