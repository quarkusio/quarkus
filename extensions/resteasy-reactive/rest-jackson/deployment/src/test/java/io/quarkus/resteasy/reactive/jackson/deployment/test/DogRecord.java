package io.quarkus.resteasy.reactive.jackson.deployment.test;

public record DogRecord(String name, int age) {
    public DogRecord() {
        this(null, 0);
    }
}
