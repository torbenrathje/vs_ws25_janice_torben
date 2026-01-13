package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class Client {
    public static final String DESTINATION_IP = "localhost";
    //public static final String DESTINATION_IP = "192.168.178.29";
    //public static final String DESTINATION_IP = "10.88.109.202";

    public static final int[] DESTINATION_PORTS = new int[]{44444, 44445, 44446, 44447};

    public static void main(String[] args) {
        List<ServerAddress> list = new ArrayList<>();
        list.add(new ServerAddress(DESTINATION_IP, DESTINATION_PORTS[0]));

        Datastore client = new ClientStub(list);
        client.write(1, "B");
        System.out.println(client.read(1));

        try {
            client.read(2);
        }
        catch (NoSuchElementException e) {
            System.err.println("No Such Element");
        }
    }

}
