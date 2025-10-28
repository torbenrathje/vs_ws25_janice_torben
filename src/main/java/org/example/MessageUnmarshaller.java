package org.example;

import com.google.gson.*;
import org.example.enums.MessageType;
import org.example.message.AbstractMessage;
import org.example.message.MessageRequest;
import org.example.message.MessageResponse;

import java.lang.reflect.Type;

public class MessageUnmarshaller implements JsonDeserializer<AbstractMessage> {

    @Override
    public AbstractMessage deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        MessageType type = MessageType.valueOf(obj.get("type").getAsString().toUpperCase());

        if (type == MessageType.REQUEST) {
            return context.deserialize(json, MessageRequest.class);
        } else if (type == MessageType.RESPONSE) {
            return context.deserialize(json, MessageResponse.class);
        } else {
            throw new JsonParseException("Unknown message type: " + type);
        }
    }
}