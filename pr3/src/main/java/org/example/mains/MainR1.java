package org.example.mains;

import org.example.RobotNode;

import static org.example.RegistryServer.REGISTRY_HOST;

public class MainR1 {
    static void main() {
        RobotNode robot1 = new RobotNode("robot1", REGISTRY_HOST, 10101, true);
        robot1.start();
    }
}
