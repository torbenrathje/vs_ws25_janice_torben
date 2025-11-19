package org.example;

public class RPCvsLocal {
    static final int NUM_RUNS = 500;

    public static void main(String[] args) {
        Thread serverThread = new Thread(() -> Server.main(new String[]{}));
        serverThread.setDaemon(true); // schließt bei testende
        serverThread.start();

        //server muss laufen
        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }


        Datastore client = new ClientStub(Client.DESTINATION_IP, Client.DESTINATION_PORT);
        Datastore local = new ServerDatastore();

        long startClient = System.nanoTime();
        for(int i = 0; i < NUM_RUNS; i++){
            client.write(i, "RPS run:" + i);
            client.read(i);
        }
        long endClient = System.nanoTime();

        long startLocal = System.nanoTime();
        for(int i = 0; i < NUM_RUNS; i++){
            local.write(i, "local run:" + i);
            local.read(i);
        }
        long endLocal = System.nanoTime();

        long resultClient = (endClient - startClient) /1000000;
        long resultLocal = (endLocal - startLocal) /1000000;

        System.out.println("Dauer von " + NUM_RUNS + " RPC Methodenaufrufe betraegt: " + resultClient + " ms");
        System.out.println("Dauer von " + NUM_RUNS + " lokalen Methodenaufrufe betraegt: " + resultLocal + " ms");

    }
}
