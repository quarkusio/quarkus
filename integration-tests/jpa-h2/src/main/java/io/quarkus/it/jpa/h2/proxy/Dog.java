package io.quarkus.it.jpa.h2.proxy;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import org.hibernate.annotations.Proxy;

@Entity
@Proxy(proxyClass = DogProxy.class)
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
