package io.quarkus.it.pulsar;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Fruit {

    public String name;

    public Fruit(String name) {
        this.name = name;
    }

    public Fruit() {
        // Jackson will uses this constructor
    }

    @Override
    public String toString() {
        return "Fruit{" +
                "name='" + name + '\'' +
                '}';
    }
}
