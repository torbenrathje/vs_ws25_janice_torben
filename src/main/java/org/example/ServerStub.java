package org.example;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.example.enums.MessageError;
import org.example.enums.MessageType;
import org.example.message.AbstractMessage;
import org.example.message.MessageRequest;
import org.example.message.MessageResponse;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.NoSuchElementException;

public class ServerStub {
/*
server bekommt nachricht
checken ob nachrichtenformat okay?
an server proezdur weiterleiten
von dem bekommt er result
davon nachrichtentyp erstellen
übers netzwerk schicken
 */


    private Datastore datastore;

    public ServerStub(Datastore dataStoreServer) {
        datastore = dataStoreServer;
    }

    public void startServer(int port) throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);

        //ServerSocket serverSocket = new ServerSocket();
        InetAddress localAddress = InetAddress.getLocalHost();
        System.out.println("server runs on: " + localAddress.getHostAddress());
        //serverSocket.bind(new InetSocketAddress(localAddress, port));

        while (true) {
            Socket clientSocket = serverSocket.accept();
            new Thread(() -> handleClient(clientSocket)).start();
        }
    }

    private void handleClient(Socket socket){
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String jsonRequest = in.readLine();

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(AbstractMessage.class, new MessageUnmarshaller())
                    .create();

            AbstractMessage messageRequest = gson.fromJson(jsonRequest, AbstractMessage.class);
            System.out.println(messageRequest);


            //ab jetzt nachricht erstellt und evtl fehler weitergegeben
            String result = "";
            MessageType type = MessageType.RESPONSE;
            MessageError error = null;
            int id;

            if (messageRequest instanceof MessageRequest res) {
                id = res.id;
                if (res.method.equals("write")) {
                    int index;
                    String data;
                    if (res.params.length == 2) {
                        if (res.params[0] instanceof Number n && n.doubleValue() % 1 == 0 && res.params[1] instanceof String s) {
                            //gson wandelt alle zahlen in double um, daher muss man das so umwandeln
                            index = n.intValue(); // in int umwandeln
                            data = s;
                            datastore.write(index, data);
                        }
                        else {
                            error = MessageError.PARAMETER_TYPE_INVALID;
                        }
                    }
                    else {
                        error = MessageError.PARAMETER_NUM_INVALID;
                    }

                }
                else if (res.method.equals("read")) {
                    int index;
                    if (res.params.length == 1) {
                        if (res.params[0] instanceof Number n && n.doubleValue() % 1 == 0) {
                            //gson wandelt alle zahlen in double um, daher muss man das so umwandeln
                            index = n.intValue(); // in int umwandeln
                            try {
                                result = datastore.read(index);
                            }
                            catch (NoSuchElementException e)//index gar nicht beim server Datastore vorhanden
                            {
                                error = MessageError.NO_SUCH_ELEMENT;
                            }
                        }
                        else {
                            error = MessageError.PARAMETER_TYPE_INVALID;
                        }
                    }
                    else {
                        error = MessageError.PARAMETER_NUM_INVALID;
                    }

                }
                else {
                    error = MessageError.METHOD_NAME_INVALID;
                }

                MessageResponse messageResponse = new MessageResponse(type, id, result, error);
                System.out.println(messageResponse);
                out.println(messageResponse.toJson());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
