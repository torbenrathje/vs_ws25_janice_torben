package org.example;

import org.junit.jupiter.api.*;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
public class IntegrationsTest {

    @BeforeAll
    static void startServer() {
        Thread serverThread = new Thread(() -> Server.main(new String[]{}));
        serverThread.setDaemon(true); // schließt bei testende
        serverThread.start();

        //server muss laufen
        try { Thread.sleep(500); } catch (InterruptedException e) { e.printStackTrace(); }
    }

    @Test
    void testWriteAndReadSingleValue() {
        Datastore client = new ClientStub(Client.DESTINATION_IP, Client.DESTINATION_PORT);

        client.write(0, "test");
        String result = client.read(0);

        assertEquals("test", result);
    }

    @Test
    void testWriteAndReadTwoValues() {
        Datastore client = new ClientStub(Client.DESTINATION_IP, Client.DESTINATION_PORT);

        client.write(1, "test1");
        client.write(2, "test2");
        String result1 = client.read(1);
        String result2 = client.read(2);

        assertEquals("test1", result1);
        assertEquals("test2", result2);
    }

    @Test
    void testOverwriteValue() {
        Datastore client = new ClientStub(Client.DESTINATION_IP, Client.DESTINATION_PORT);

        client.write(1, "a");
        client.write(1, "b");
        String result = client.read(1);

        assertEquals("b", result);
    }

    @Test
    void testNoSuchElement() {
        Datastore client = new ClientStub(Client.DESTINATION_IP, Client.DESTINATION_PORT);

        assertThrows(NoSuchElementException.class, () -> client.read(999));
    }

}
