package org.example;

import java.io.IOException;

import static org.example.Client.DESTINATION_PORT;

public class Server {

    public static void main(String[] args) {
        Datastore serverDatastore = new ServerDatastore();
        ServerStub serverStub = new ServerStub(serverDatastore);
        try {
            serverStub.startServer(DESTINATION_PORT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
