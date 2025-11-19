package org.example;

public class ValueMessage {
    public enum Type {INIT, UPDATE}

    private Type type;
    private String senderId;
    private int value;

    public ValueMessage(Type type, String senderId, int value){
        this.type = type;
        this.senderId = senderId;
        this.value = value;
    }

    public Type getType() {
        return type;
    }

    public String getSenderId() {
        return senderId;
    }

    public int getValue() {
        return value;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public void setValue(int value) {
        this.value = value;
    }
}
