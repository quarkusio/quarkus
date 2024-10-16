package io.quarkus.resteasy.reactive.jackson.deployment.test;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Dog extends AbstractNamedPet {

    @JsonProperty("age")
    private int publicAge;

    @JsonProperty("vaccinated")
    private Boolean publicVaccinated;

    public int getPublicAge() {
        return publicAge;
    }

    public void setPublicAge(int publicAge) {
        this.publicAge = publicAge;
    }

    public Boolean getPublicVaccinated() {
        return publicVaccinated;
    }

    public void setPublicVaccinated(Boolean publicVaccinated) {
        this.publicVaccinated = publicVaccinated;
    }
}
