package io.quarkus.resteasy.reactive.jackson.deployment.test;

public class UnsecuredPet extends AbstractUnsecuredPet {

    private Veterinarian veterinarian;

    public Veterinarian getVeterinarian() {
        return veterinarian;
    }

    public void setVeterinarian(Veterinarian veterinarian) {
        this.veterinarian = veterinarian;
    }
}
