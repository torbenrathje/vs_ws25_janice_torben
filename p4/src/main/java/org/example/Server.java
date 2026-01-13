package org.example;

import java.io.IOException;

import static org.example.Client.DESTINATION_PORTS;

public class Server {

    private Datastore serverDatastore;
    private ServerStub serverStub;
    private int port;

    public Server(int port) {
        this.port = port;
        serverDatastore = new ServerDatastore();
        serverStub = new ServerStub(serverDatastore);
    }

    public static void main(String[] args) {
        Server server = new Server(DESTINATION_PORTS[0]);
        server.startServer();
    }

    public void startServer() {

        try {
            serverStub.startServer(port);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stopServer() {
        serverStub.stopServer();
    }
}
