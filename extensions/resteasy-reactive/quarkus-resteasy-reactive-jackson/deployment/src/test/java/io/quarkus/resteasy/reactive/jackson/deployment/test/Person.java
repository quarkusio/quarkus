package io.quarkus.resteasy.reactive.jackson.deployment.test;

import javax.validation.constraints.NotBlank;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Person {

    private String first;

    @NotBlank(message = "Title cannot be blank")
    @SecureField(rolesAllowed = "admin")
    private String last;

    public String getFirst() {
        return first;
    }

    public void setFirst(String first) {
        this.first = first;
    }

    public String getLast() {
        return last;
    }

    public void setLast(String last) {
        this.last = last;
    }
}
