package io.quarkus.resteasy.reactive.jackson.deployment.test;

public abstract class AbstractPet implements SecuredPersonInterface {

    private String publicName;
    private Veterinarian veterinarian;

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
