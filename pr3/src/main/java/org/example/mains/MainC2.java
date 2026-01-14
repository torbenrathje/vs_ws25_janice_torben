package org.example.mains;

import org.example.Config;
import org.example.RegistryServer;
import org.example.TerminalClient;

public class MainC2 {

    public static void main(String[] args) throws Exception {
        String name = "client2";
        int port = 10001;
        TerminalClient client = new TerminalClient(name, Config.OWN_IP, port);
        client.start();
    }
}
