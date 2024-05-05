package io.quarkus.amazon.lambda.deployment.testing;

public class Person {

    public Person() {
    }

    public Person(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }
}
