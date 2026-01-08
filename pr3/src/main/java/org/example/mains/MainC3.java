package org.example.mains;

import org.example.TerminalClient;

public class MainC3 {

    public static void main(String[] args) throws Exception {
        String name = "client3";
        String ip = "127.0.0.1";
        int port = 10002;

        TerminalClient client = new TerminalClient(name, ip, port);
        client.start();
    }
}
