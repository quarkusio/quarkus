package io.quarkus.resteasy.reactive.server.test.security;

public class SerializationEntity {
    private String name;

    public String getName() {
        return name;
    }

    public SerializationEntity setName(String name) {
        this.name = name;
        return this;
    }
}
