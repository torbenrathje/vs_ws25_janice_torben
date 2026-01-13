package org.example.message;

import com.google.gson.Gson;
import org.example.enums.MessageType;

public abstract class AbstractMessage {
    public MessageType type;
    public int id;

    public AbstractMessage(MessageType type, int id) {
        this.type = type;
        this.id = id;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
