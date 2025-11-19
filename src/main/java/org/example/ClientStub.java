package org.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.NoSuchElementException;
import com.google.gson.*;
import org.example.enums.MessageType;
import org.example.message.AbstractMessage;
import org.example.message.MessageRequest;
import org.example.message.MessageResponse;

public class ClientStub implements Datastore{

    private String destinationIp;
    private int port;

    private int currentId;

    public ClientStub(String destinationIp, int port) {
        this.destinationIp = destinationIp;
        this.port = port;
        currentId = 0;
    }
    @Override
    public void write(int index, String data) {
        MessageRequest request = new MessageRequest(MessageType.REQUEST, currentId++, "write", new Object[]{index, data});
        //System.out.println(request);
        sendRequestToIp(request);
    }

    @Override
    public String read(int index) throws NoSuchElementException {
        MessageRequest request = new MessageRequest(MessageType.REQUEST, currentId++, "read", new Object[]{index});
        //System.out.println(request);
        return sendRequestToIp(request);
    }

    private String sendRequestToIp(MessageRequest message) throws NoSuchElementException{

        try (Socket socket = new Socket(destinationIp, port);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

                out.println(message.toJson());


                Gson gson = new GsonBuilder()
                        .registerTypeAdapter(AbstractMessage.class, new MessageUnmarshaller())
                        .create();

                String jsonResponse = in.readLine();

                AbstractMessage messageResponse = gson.fromJson(jsonResponse, AbstractMessage.class);
                //TODO fehlerbehandlung
                if (messageResponse instanceof MessageResponse res) {
                    //System.out.println(res);
                    if (res.error != null)
                    {
                        switch (res.error) {
                            case NO_SUCH_ELEMENT -> throw new NoSuchElementException();
                            case METHOD_NAME_INVALID -> System.err.println("Method Name Invalid");
                            case PARAMETER_NUM_INVALID -> System.err.println("Number of Parameters Invalid");
                            case PARAMETER_TYPE_INVALID -> System.err.println("Parameter Type Invalid");
                            case SERVER_NOT_REACHABLE -> System.err.println("Server Not Reachable");
                            default -> System.err.println("Unknown Exception");
                        }
                    }
                    return res.result;
                }
                else {
                    return "";
                }

        } catch (IOException e) {
            System.err.println("Server Not Reachable");
            return "";
        }
    }
}
