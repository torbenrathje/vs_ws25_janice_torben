package org.example.message;

import com.google.gson.Gson;
import org.example.enums.MessageType;

import java.util.Arrays;

public class MessageRequest extends AbstractMessage {
    public String method;
    public Object[] params;

    public MessageRequest(MessageType type, int id, String method, Object[] params) {
        super(type, id);
        this.method = method;
        this.params = params;
    }

    public static MessageRequest fromJson(String json) {
        Gson gson = new Gson();

        return gson.fromJson(json, MessageRequest.class);
    }

    @Override
    public String toString() {
        return "MessageRequest{" +
                "method='" + method + '\'' +
                ", params=" + Arrays.toString(params) +
                ", type=" + type +
                ", id=" + id +
                '}';
    }
}
