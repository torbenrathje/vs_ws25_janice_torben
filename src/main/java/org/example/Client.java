package org.example;

public class Client {
    public static final String DESTINATION_IP = "localhost";
    //public static final String DESTINATION_IP = "192.168.178.29";

    public static final int DESTINATION_PORT = 44444;

    public static void main(String[] args) {
        Datastore client = new ClientStub(DESTINATION_IP, DESTINATION_PORT);
        client.write(1, "B");
        System.out.println(client.read(1));
    }

}
