package org.example.mains;

import org.example.TerminalClient;

public class MainC1 {

    public static void main(String[] args) throws Exception {
        // Default-Werte
        String name = "client1";
        String ip = "127.0.0.1";
        int port = 10000;

        TerminalClient client = new TerminalClient(name, ip, port);
        client.start();
    }
}
