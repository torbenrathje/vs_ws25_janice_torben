package org.example;

import java.util.*;
import java.util.stream.IntStream;

public class Main {

    //TODO Runconfig: 1 1 5 108 76 12 60 36

    public static final int WARTEZEIT_MAIN =8_000;
    public static final String ADDRESS_BROKER = "tcp://localhost:1883"; // 1883 auf mosquitto / active mq
    public static final String ADDRESS_BROKER_REMOTE = "tcp://172.20.10.3:1883"; //61616 standard active mq port


    /**
     * 2 0 5 108 76 12 60 36 startargumente
     * ; ANzhal gesamt rechner; "rechner ID fängt bei 0 an", anzahl an prozessen; liste mit zahlen,( dynamisch)
     *
     * liste durchgehen in schleife; rechner 1 erster wert, rechner 2 zweiter wert, von vorne bis ende erreicht
     */

    public static void main(String[] args) {
        try {
            for (String arg : args)
            {
                System.out.println(arg);
            }

            int numPc = -1;
            int pcId = -1;
            int numProc;
            List<Integer> numbers = new ArrayList<>();


            numPc = Integer.parseInt(args[0]);
            pcId = Integer.parseInt(args[1]);
            numProc= Integer.parseInt(args[2]);


            String brokerAddress;
            if (pcId == 0) {
                brokerAddress = ADDRESS_BROKER;
            }
            else
            {
                brokerAddress = ADDRESS_BROKER_REMOTE;
            }

            for (int i = 3; i < numProc+3; i++)
            {
                numbers.add(Integer.parseInt(args[i]));
            }


            // ---Master erstellen--- (nur auf ersten PC)
            MqttConnection masterConnection = null;
            if (pcId == 0) {
                masterConnection = new MqttConnection(ADDRESS_BROKER, "master");
                MasterCoordinator masterCoordinator = new MasterCoordinator(masterConnection, numProc);

                Thread masterThread = new Thread(() -> {
                    try {
                        masterCoordinator.start();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
                masterThread.setDaemon(true); // schließt bei ende
                masterThread.start();

            }





            Map<Integer, MqttConnection> connectionMap = new HashMap<>();
            // Create MQTT clients
            for (int i = pcId; i < numProc; i+=numPc)
            {
                try {
                    connectionMap.put(i, new MqttConnection(brokerAddress, "p" + i));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }



            HashMap<RingProcess, Integer> ringProcessHashMap = new HashMap<>();
            for (int i = pcId; i < numProc; i+=numPc)
            {
                ringProcessHashMap.put(new RingProcess("p"+i, numbers.get(i), null, null, connectionMap.get(i)), i);
            }



            // Ring verbinden
            for (RingProcess ringProcess : ringProcessHashMap.keySet()) {
                int i = ringProcessHashMap.get(ringProcess);
                String prev = "p" + ((i - 1 + numProc) % numProc);
                String next = "p" + ((i + 1) % numProc);

                ringProcess.setNext(next);
                ringProcess.setPrev(prev);
            }

            // --- Läuft automatisch durch Startnachrichten ---

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

}
