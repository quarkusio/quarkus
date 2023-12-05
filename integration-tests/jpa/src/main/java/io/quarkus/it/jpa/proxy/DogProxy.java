package io.quarkus.it.jpa.proxy;

public interface DogProxy {

    String bark();

    String getFavoriteToy();

    void setFavoriteToy(String favoriteToy);
}
