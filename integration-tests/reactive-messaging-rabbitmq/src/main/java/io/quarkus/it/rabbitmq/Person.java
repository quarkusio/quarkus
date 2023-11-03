package io.quarkus.it.rabbitmq;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Person {
    private String name;

    public Person() {
        // default no-arg constructor.
    }

    public Person(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }
}
