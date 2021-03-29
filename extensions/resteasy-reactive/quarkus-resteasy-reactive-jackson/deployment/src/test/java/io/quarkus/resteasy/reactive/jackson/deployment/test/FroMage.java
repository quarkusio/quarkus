package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class FroMage {
    public String name;

    // required for Jackson
    // public FroMage() {}
    public FroMage(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "FroMage: " + name;
    }
}
