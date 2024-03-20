package io.quarkus.resteasy.reactive.jackson.deployment.test;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Cat extends AbstractNamedPet {

    @SecureField(rolesAllowed = "admin")
    private int privateAge;

    public int getPrivateAge() {
        return privateAge;
    }

    public void setPrivateAge(int privateAge) {
        this.privateAge = privateAge;
    }
}
