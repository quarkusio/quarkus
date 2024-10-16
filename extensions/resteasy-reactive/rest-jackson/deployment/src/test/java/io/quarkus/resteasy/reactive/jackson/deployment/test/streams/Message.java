package io.quarkus.resteasy.reactive.jackson.deployment.test.streams;

public class Message {
    public String name;

    public Message(String name) {
        this.name = name;
    }

    // for Jsonb
    public Message() {
    }
}
