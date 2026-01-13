package org.example;

import java.util.ArrayList;
import java.util.List;

import static org.example.Client.DESTINATION_IP;

public class MainTest {


    public static void main(String[] args) throws Exception{
        List<ServerAddress> servers = new ArrayList<>(List.of(
                new ServerAddress(DESTINATION_IP, 44444),
                new ServerAddress(DESTINATION_IP, 44445)

        ));



        //List<Thread> serverThreads = new ArrayList<>();
        List<Server> serverList = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            int port = servers.get(i).port();

            Server server = new Server(port);

            Thread serverThread = new Thread(() -> server.startServer());
            serverThread.setDaemon(true); // schließt bei testende
            serverThread.start();

            //serverThreads.add(serverThread);
            serverList.add(server);
        }


        //server muss laufen
        Thread.sleep(500);


        Datastore client = new ClientStub(servers);

        client.write(0, "A");
        client.read(0);//sollte beim ersten Server lesen

        serverList.get(1).stopServer();

        client.read(0); //sollte laut round robin versuchen beim zweiten zu lesen

        serverList.get(0).stopServer();

        client.read(0);
    }

}
