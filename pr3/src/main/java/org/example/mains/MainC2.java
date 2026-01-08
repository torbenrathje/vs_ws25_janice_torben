package org.example.mains;

import org.example.TerminalClient;

public class MainC2 {

    public static void main(String[] args) throws Exception {
        String name = "client2";
        String ip = "127.0.0.1";
        int port = 10001;

        TerminalClient client = new TerminalClient(name, ip, port);
        client.start();
    }
}
