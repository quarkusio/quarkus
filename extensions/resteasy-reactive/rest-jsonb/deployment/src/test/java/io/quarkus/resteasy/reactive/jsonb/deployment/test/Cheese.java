package io.quarkus.resteasy.reactive.jsonb.deployment.test;

public class Cheese {

    private final String name;

    public Cheese(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "Cheese: " + name;
    }

    public String getName() {
        // explicitly cause an exception to ensure that an exception during json writing is properly handled
        throw new RuntimeException("Fake exception during serialization");
    }
}
