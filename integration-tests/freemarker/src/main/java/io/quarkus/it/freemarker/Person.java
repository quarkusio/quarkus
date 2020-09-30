package io.quarkus.it.freemarker;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Person {

    public String name;

    public Person setName(String name) {
        this.name = name;
        return this;
    }
}
