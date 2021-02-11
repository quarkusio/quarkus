package io.quarkus.it.jpa.h2.proxy;

public interface DogProxy {

    String bark();

    String getFavoriteToy();

    void setFavoriteToy(String favoriteToy);
}
