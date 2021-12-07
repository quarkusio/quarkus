package io.quarkus.it.kafka;

public class Fruit {

    public String name;

    public Fruit(String name) {
        this.name = name;
    }

    public Fruit() {
        // Jackson will uses this constructor
    }
}
