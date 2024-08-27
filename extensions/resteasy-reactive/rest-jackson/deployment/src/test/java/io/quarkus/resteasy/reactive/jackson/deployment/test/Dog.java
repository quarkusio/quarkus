package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Dog extends AbstractNamedPet {

    @JsonProperty("age")
    private int publicAge;

    public int getPublicAge() {
        return publicAge;
    }

    public void setPublicAge(int publicAge) {
        this.publicAge = publicAge;
    }
}
