package org.example.mains;

import org.example.Config;
import org.example.RobotNode;

import static org.example.RegistryServer.REGISTRY_HOST;

public class MainR2 {
    public static void main(String[] args) {
        RobotNode robot2 = new RobotNode("robot2", Config.OWN_IP, 10102, true);
        robot2.start();
    }
}
