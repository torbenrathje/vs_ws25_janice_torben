package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.NoSuchElementException;
import com.google.gson.*;
import org.example.enums.MessageType;
import org.example.exceptions.ServerNotReachableException;
import org.example.message.AbstractMessage;
import org.example.message.MessageRequest;
import org.example.message.MessageResponse;

public class ClientStub implements Datastore{

    private List<ServerAddress> servers;
    private int currentId;
    private int roundRobin;

    public ClientStub(List<ServerAddress> servers) {
        this.servers = servers;
        currentId = 0;
        roundRobin = 0;
    }
    @Override
    public void write(int index, String data) {
        MessageRequest request = new MessageRequest(MessageType.REQUEST, currentId++, "write", new Object[]{index, data});
        //System.out.println(request);
        sendRequestToAllIps(request);
    }

    @Override
    public String read(int index) throws NoSuchElementException {
        if(servers.size() == 0){
            throw new RuntimeException("Kein Server vorhanden");
        }
        ServerAddress server = servers.get(roundRobin);
        if (Config.DEBUG) {
            System.out.println("Read bei Server mit id: " + roundRobin);
        }

        roundRobin = (roundRobin + 1) % servers.size();

        MessageRequest request = new MessageRequest(MessageType.REQUEST, currentId++, "read", new Object[]{index});

        String result = "";
        try {
            result = sendRequestToIp(request, server);
        }
        catch (ServerNotReachableException e) {//Server ist ausgefallen -> nochmal
            return read(index);
        }
        return result;
    }

    private void sendRequestToAllIps(MessageRequest message) throws NoSuchElementException {
        for (ServerAddress serverAddress : servers) {
            try {
                sendRequestToIp(message, serverAddress);
            }
            catch (RuntimeException e) {
                throw e;
            }
        }
    }

    private String sendRequestToIp(MessageRequest message, ServerAddress server) throws NoSuchElementException, ServerNotReachableException{

        try (Socket socket = new Socket(server.ip(), server.port());
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(message.toJson());


                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(AbstractMessage.class, new MessageUnmarshaller())
                        .create();

                String jsonResponse = in.readLine();

                AbstractMessage messageResponse = gson.fromJson(jsonResponse, AbstractMessage.class);
                if (messageResponse instanceof MessageResponse res) {
                    if (Config.DEBUG) {
                        System.out.println("Response :" + res);
                    }
                    if (res.error != null)
                    {
                        switch (res.error) {
                            case NO_SUCH_ELEMENT -> throw new NoSuchElementException();
                            case METHOD_NAME_INVALID -> throw new RuntimeException("Method Name Invalid");
                            case PARAMETER_NUM_INVALID -> throw new RuntimeException("Number of Parameters Invalid");
                            case PARAMETER_TYPE_INVALID -> throw new RuntimeException("Parameter Type Invalid");
                            case SERVER_NOT_REACHABLE -> {
                                servers.remove(server);
                                if (Config.DEBUG) {
                                    System.err.println("Server Not Reachable");
                                }
                                throw new ServerNotReachableException("Server Not Reachable");
                            }
                            default -> throw new RuntimeException("Unknown Exception");
                        }
                    }
                    return res.result;
                }
                else {
                    return "";
                }

        } catch (IOException e) {
            servers.remove(server);
            if (Config.DEBUG) {
                System.err.println("Server Not Reachable");
            }
            throw new ServerNotReachableException("Server Not Reachable");
        }
    }
}
