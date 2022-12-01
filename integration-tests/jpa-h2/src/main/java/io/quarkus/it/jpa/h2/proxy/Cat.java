package io.quarkus.it.jpa.h2.proxy;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("CAT")
public class Cat extends Pet {

    @Override
    public String makeNoise() {
        return "Meow";
    }
}
