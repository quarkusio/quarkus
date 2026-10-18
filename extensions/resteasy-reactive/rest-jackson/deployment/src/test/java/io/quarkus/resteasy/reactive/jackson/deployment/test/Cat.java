package io.quarkus.resteasy.reactive.jackson.deployment.test;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Cat extends AbstractNamedPet {

    private int privateAge;

    @SecureField(rolesAllowed = "admin")
    public int getPrivateAge() {
        return privateAge;
    }

    public void setPrivateAge(int privateAge) {
        this.privateAge = privateAge;
    }

    public char getInitial() {
        return getPublicName().charAt(0);
    }
}
