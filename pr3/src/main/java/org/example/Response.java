package org.example;

public class Response<T> {
    private String status;
    private T payload;
    private String message;

    public Response(String status, T payload, String message) {
        this.status = status;
        this.payload = payload;
        this.message = message;
    }
}

