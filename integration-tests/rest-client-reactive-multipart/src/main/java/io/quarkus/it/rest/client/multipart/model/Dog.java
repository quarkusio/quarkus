package io.quarkus.it.rest.client.multipart.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.resteasy.reactive.jackson.SecureField;

public class Dog {

    @JsonProperty("age")
    private int publicAge;

    @SecureField(rolesAllowed = "admin")
    private String privateName;

    private String publicName;

    private Veterinarian veterinarian;

    public int getPublicAge() {
        return publicAge;
    }

    public void setPublicAge(int publicAge) {
        this.publicAge = publicAge;
    }

    public String getPrivateName() {
        return privateName;
    }

    public void setPrivateName(String privateName) {
        this.privateName = privateName;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public Veterinarian getVeterinarian() {
        return veterinarian;
    }

    public void setVeterinarian(Veterinarian veterinarian) {
        this.veterinarian = veterinarian;
    }
}
