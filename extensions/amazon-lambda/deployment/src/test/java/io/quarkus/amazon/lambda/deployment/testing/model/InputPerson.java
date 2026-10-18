package io.quarkus.amazon.lambda.deployment.testing.model;

public class InputPerson {

    public InputPerson() {
    }

    public InputPerson(String name) {
        this.name = name;
    }

    private String name;

    public String getName() {
        return name;
    }

    public InputPerson setName(String name) {
        this.name = name;
        return this;
    }
}
