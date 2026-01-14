package org.example.mains;

import org.example.Config;
import org.example.RobotNode;

import static org.example.RegistryServer.REGISTRY_HOST;

public class MainR1 {
    public static void main(String[] args) {
        RobotNode robot1 = new RobotNode("robot1", Config.OWN_IP, 10101, true);
        robot1.start();
    }
}
