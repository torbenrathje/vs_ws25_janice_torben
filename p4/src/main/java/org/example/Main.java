package org.example;

import java.util.List;

import static org.example.Client.DESTINATION_IP;

public class Main {
    static final int NUM_RUNS = 100;


    //TODO erstmal MainServer auf allen Rechnern starten
    public static void main(String[] args) {

        List<ServerAddress> servers = Config.SERVER_LIST;

        //

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

        for (int j = 0; j< servers.size(); j++) {
            client = new ClientStub(servers.subList(0, j+1));

            startClient = System.nanoTime();
            for(int i = 0; i < NUM_RUNS; i++){
                client.write(i, "RPS run:" + i);
                client.read(i);
            }
            endClient = System.nanoTime();


            resultClient = (endClient - startClient) /1000000;

            System.out.println("Dauer von " + NUM_RUNS + " RPC Methodenaufrufe betraegt: " + resultClient + " ms bei " + (j+1) + " Anzahl an Servern" );
        }
    }
}
