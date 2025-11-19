package org.example.message;

import com.google.gson.Gson;
import org.example.enums.MessageError;
import org.example.enums.MessageType;

public class MessageResponse extends AbstractMessage {

    public String result;

    public MessageError error;

    public MessageResponse(MessageType type, int id, String result, MessageError error) {
        super(type, id);
        this.result = result;
        this.error = error;
    }

    public static MessageResponse fromJson(String json) {
        Gson gson = new Gson();

        return gson.fromJson(json, MessageResponse.class);
    }

    @Override
    public String toString() {
        return "MessageResponse{" +
                "result='" + result + '\'' +
                ", error=" + error +
                ", type=" + type +
                ", id=" + id +
                '}';
    }
}
