package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class Dog extends AbstractNamedPet {

    private int publicAge;

    public int getPublicAge() {
        return publicAge;
    }

    public void setPublicAge(int publicAge) {
        this.publicAge = publicAge;
    }
}
