package io.quarkus.it.jpa.proxy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("DOG")
public class Dog extends Pet implements DogProxy {

    private String favoriteToy;

    @Override
    public String makeNoise() {
        return bark();
    }

    @Override
    public String bark() {
        return "Woof";
    }

    public String getFavoriteToy() {
        return favoriteToy;
    }

    public void setFavoriteToy(String favoriteToy) {
        this.favoriteToy = favoriteToy;
    }
}
