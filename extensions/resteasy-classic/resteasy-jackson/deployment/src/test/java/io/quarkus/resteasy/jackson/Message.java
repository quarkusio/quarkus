package io.quarkus.resteasy.jackson;

public class Message {

    private String message;

    public String getMessage() {
        return message;
    }

    public Message setMessage(String message) {
        this.message = message;
        return this;
    }
}
