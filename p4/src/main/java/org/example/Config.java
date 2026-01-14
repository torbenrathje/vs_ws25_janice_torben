package org.example;

import java.util.List;

import static org.example.Client.DESTINATION_IP;

public class Config {
    public static final boolean DEBUG = false;

    public static final String OWN_IP = "172.20.10.2";

    public static final List<ServerAddress> SERVER_LIST = List.of(
            new ServerAddress(OWN_IP, 44444),
            new ServerAddress(OWN_IP, 44445),
            new ServerAddress(DESTINATION_IP, 44446),
            new ServerAddress(DESTINATION_IP, 44447)

    );
}

