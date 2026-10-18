package io.quarkus.it.kafka.streams;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Category {

    public String name;
    public String value;

    public Category() {
    }

    public Category(String name, String value) {
        this.name = name;
        this.value = value;
    }
}
