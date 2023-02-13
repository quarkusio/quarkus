package io.quarkus.it.spring.web;

import jakarta.validation.constraints.NotBlank;

public class Person {

    @NotBlank
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
