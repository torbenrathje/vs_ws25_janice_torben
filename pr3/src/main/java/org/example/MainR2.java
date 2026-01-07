package org.example;

import static org.example.RegistryServer.REGISTRY_HOST;

public class MainR2 {
    static void main() {
        RobotNode robot2 = new RobotNode("robot2", REGISTRY_HOST, 10102, true);
        robot2.start();
    }
}
