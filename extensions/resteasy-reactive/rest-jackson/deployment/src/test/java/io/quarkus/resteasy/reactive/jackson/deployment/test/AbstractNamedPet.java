package io.quarkus.resteasy.reactive.jackson.deployment.test;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public abstract class AbstractNamedPet extends AbstractPet {

    @SecureField(rolesAllowed = "admin")
    private String privateName;

    public String getPrivateName() {
        return privateName;
    }

    public void setPrivateName(String privateName) {
        this.privateName = privateName;
    }
}
