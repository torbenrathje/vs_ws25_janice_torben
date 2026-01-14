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

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public String toString() {
        return "Entry{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", type=" + type +
                '}';
    }
}
