package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class Cheese {

    // private is what's causing the exception
    private final String name;

    public Cheese(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Cheese: " + name;
    }
}
