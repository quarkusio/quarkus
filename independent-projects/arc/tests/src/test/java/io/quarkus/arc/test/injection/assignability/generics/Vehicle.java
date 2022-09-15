package io.quarkus.arc.test.injection.assignability.generics;

import jakarta.inject.Inject;

public abstract class Vehicle<T extends Engine> {

    @Inject
    T eng;

    public T getEngine() {
        return eng;
    }
}
