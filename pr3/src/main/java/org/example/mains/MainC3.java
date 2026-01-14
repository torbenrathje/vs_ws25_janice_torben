package org.example.mains;

import org.example.Config;
import org.example.TerminalClient;

public class MainC3 {

    public static void main(String[] args) throws Exception {
        String name = "client3";
        int port = 10002;

        TerminalClient client = new TerminalClient(name, Config.OWN_IP, port);
        client.start();
    }
}
