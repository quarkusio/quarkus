package io.quarkus.resteasy.reactive.jackson.deployment.test;

public abstract class AbstractUnsecuredPet {

    private String publicName;

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

}
