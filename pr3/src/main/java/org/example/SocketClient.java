package org.example;

import com.google.gson.Gson;
import org.example.communication.Request;

import java.io.*;
import java.net.Socket;
import java.util.Map;

public class SocketClient {

    private final Gson gson = new Gson();

    /**
     * Sendet ein Request-Map an den Server und bekommt die Response zurück als Map
     */
    public Map<String,Object> sendRequest(String host, int port, Request<?, ?> request) throws IOException {
        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String jsonRequest = gson.toJson(request);
            //System.out.println(jsonRequest);
            out.println(jsonRequest);

            String jsonResponse = in.readLine();
            return gson.fromJson(jsonResponse, Map.class);
        }
    }
}
