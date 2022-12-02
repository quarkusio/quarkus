package io.quarkus.amazon.lambda.deployment.testing;

public class Person {

    private String name;

    public String getName() {
        return name;
    }

    public Person setName(String name) {
        this.name = name;
        return this;
    }
}
