package org.example;

import java.util.List;

import static org.example.Client.DESTINATION_IP;

public class RPCvsLocal {
    static final int NUM_RUNS = 500;

    public static void main(String[] args) {
        List<ServerAddress> servers = List.of(
                new ServerAddress(DESTINATION_IP, 44444),
                new ServerAddress(DESTINATION_IP, 44445),
                new ServerAddress(DESTINATION_IP, 44446),
                new ServerAddress(DESTINATION_IP, 44447)

        );

        for(int i = 0; i < servers.size(); i++){
            int port = servers.get(i).port();
            Thread serverThread = new Thread(() -> Server.main(new String[]{String.valueOf(port)}));
            serverThread.setDaemon(true); // schließt bei testende
            serverThread.start();
        }


        //server muss laufen
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Datastore client;
        long startClient;
        long endClient;
        long resultClient;

        for (int j = 0; j<4; j++) {
            client = new ClientStub(servers.subList(0, j+1));

            startClient = System.nanoTime();
            for(int i = 0; i < NUM_RUNS; i++){
                client.write(i, "RPS run:" + i);
                client.read(i);
            }
            endClient = System.nanoTime();


            resultClient = (endClient - startClient) /1000000;

            System.out.println("Dauer von " + NUM_RUNS + " RPC Methodenaufrufe betraegt: " + resultClient + " ms bei " + j + " Anzahl an Servern" );
        }
    }
}
