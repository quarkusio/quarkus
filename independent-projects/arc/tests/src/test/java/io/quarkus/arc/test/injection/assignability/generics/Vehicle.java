package io.quarkus.arc.test.injection.assignability.generics;

import javax.inject.Inject;

public abstract class Vehicle<T extends Engine> {

    @Inject
    T eng;

    public T getEngine() {
        return eng;
    }
}
