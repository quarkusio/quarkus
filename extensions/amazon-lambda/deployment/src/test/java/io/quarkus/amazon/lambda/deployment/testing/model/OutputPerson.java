package io.quarkus.amazon.lambda.deployment.testing.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OutputPerson {

    public OutputPerson() {
    }

    public OutputPerson(String name) {
        this.name = name;
    }

    @JsonProperty("outputname")
    private String name;

    public String getName() {
        return name;
    }
}
