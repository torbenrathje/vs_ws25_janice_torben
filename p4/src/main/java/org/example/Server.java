package org.example;

import java.io.IOException;

import static org.example.Client.DESTINATION_PORTS;

public class Server {


    public static void main(String[] args) {
        Datastore serverDatastore = new ServerDatastore();
        ServerStub serverStub = new ServerStub(serverDatastore);
        try {
            serverStub.startServer(DESTINATION_PORTS[0]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
