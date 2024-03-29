package io.quarkus.it.jpa.proxy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("CAT")
public class Cat extends Pet {

    @Override
    public String makeNoise() {
        return "Meow";
    }
}
