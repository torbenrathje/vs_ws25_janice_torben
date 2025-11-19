package org.example.a2;

public class MessageA2 {
    public enum Type {INIT, UPDATE}

    private Type type;
    private int senderId;
    private int value;

    public MessageA2(Type type, int senderId, int value){
        this.type = type;
        this.senderId = senderId;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public int getSenderId() {
        return senderId;
    }

    public int getValue() {
        return value;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
