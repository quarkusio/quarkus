package io.quarkus.it.rabbitmq;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Person {
    public String name;

    public Person() {
        // default no-arg constructor.
    }

    public Person(String name) {
        this.name = name;
    }
}
