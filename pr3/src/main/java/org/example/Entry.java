package org.example;

public class Entry {
    public enum Type {CLIENT, ROBOT}

    private int id;
    private String name;
    private String ip;
    private int port;
    private Type type;

    public Entry(int id, String name, String ip, int port, Type type) {
        this.id = id;
        this.name = name;
        this.ip = ip;
        this.port = port;
        this.type = type;
    }

}
