package io.quarkus.it.rest;

public class PayloadClass {

    private final String message;

    public PayloadClass(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
