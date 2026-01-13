package org.example.exceptions;

public class ServerNotReachableException extends RuntimeException {
    public ServerNotReachableException(String message) {
        super(message);
    }
}
