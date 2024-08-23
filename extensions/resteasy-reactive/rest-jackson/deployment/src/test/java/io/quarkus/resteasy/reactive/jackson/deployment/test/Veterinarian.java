package io.quarkus.resteasy.reactive.jackson.deployment.test;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Veterinarian {

    private String name;

    @SecureField(rolesAllowed = "admin")
    private String title;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
