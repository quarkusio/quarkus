package io.quarkus.resteasy.jsonb;

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
