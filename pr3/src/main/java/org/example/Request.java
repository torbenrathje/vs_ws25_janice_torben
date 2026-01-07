package org.example;

/**
 * Generische Request-Klasse
 * @param <O> Enum-Typ der Operation
 * @param <P> Payload
 */
public class Request<O extends Enum<O>, P> {
    private O operation;
    private P payload;

    public Request(O operation, P payload) {
        this.operation = operation;
        this.payload = payload;
    }

    public O getOperation() {
        return operation;
    }

    public P getPayload() {
        return payload;
    }
}


