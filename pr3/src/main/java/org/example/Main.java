package org.example;

import java.util.HashSet;
import java.util.Set;

import static org.example.RegistryServer.REGISTRY_HOST;

/**
 * Beispielaufbau:
 *
 * 1 RegistryServer
 * 2 RobotNodes (robot1, robot2)
 * 1 Client für robot1
 * 5 Clients für robot2
 */
public class Main {
    static void main() throws InterruptedException {
        RegistryServer registryServer = new RegistryServer();

        new Thread(registryServer, "RegistryServer-Thread").start();
        Thread.sleep(500);

        RobotNode robot1 = new RobotNode("robot1", REGISTRY_HOST, 10101, true);
        //RobotNode robot2 = new RobotNode("robot2", REGISTRY_HOST, 10102, true);

        robot1.start();
        //robot2.start();

        //ROBOTER müssen in eigener Main gestartet werden, wegen JavaFX

        TerminalClient clientR1 = new TerminalClient("client1", REGISTRY_HOST, 10000);

        Set<TerminalClient> clientsR2 = new HashSet<>();
        clientsR2.add(new TerminalClient("client2", REGISTRY_HOST, 10001));
        clientsR2.add(new TerminalClient("client3", REGISTRY_HOST, 10002));
        clientsR2.add(new TerminalClient("client4", REGISTRY_HOST, 10003));
        //clientsR2.add(new TerminalClient("client5", REGISTRY_HOST, 10004));
        //clientsR2.add(new TerminalClient("client6", REGISTRY_HOST, 10005));

        new Thread(clientR1, clientR1.getName()).start();
        for (TerminalClient client : clientsR2)
        {
            //new Thread(client, client.getName()).start();
        }




    }
}
