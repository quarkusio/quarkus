package io.quarkus.it.kafka.streams;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Customer {

    public int id;
    public String name;
    public int category;

    public Customer() {
    }

    public Customer(int id, String name, int category) {
        this.id = id;
        this.name = name;
        this.category = category;
    }
}
