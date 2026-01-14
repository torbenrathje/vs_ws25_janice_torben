package org.example.mains;

import org.example.Config;
import org.example.RegistryServer;
import org.example.TerminalClient;

public class MainC1 {

    public static void main(String[] args) throws Exception {
        // Default-Werte
        String name = "client1";
        int port = 10000;

        TerminalClient client = new TerminalClient(name, Config.OWN_IP, port);
        client.start();
    }
}
