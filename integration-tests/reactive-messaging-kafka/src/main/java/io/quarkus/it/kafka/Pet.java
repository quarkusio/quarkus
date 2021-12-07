package io.quarkus.it.kafka;

public class Pet {

    public String name;

    public Pet(String name) {
        this.name = name;
    }

    public Pet() {
        // Jackson will use this constructor.
    }
}
