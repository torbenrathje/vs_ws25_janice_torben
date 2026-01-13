package org.example.communication;

public class Response<T> {
    private Status status;
    private T payload;
    private String message;

    public Response(Status status, T payload, String message) {
        this.status = status;
        this.payload = payload;
        this.message = message;
    }

    public enum Status {
        OK, ERROR
    }
}

