package io.quarkus.arc.test.injection.generics;

import javax.inject.Inject;

public abstract class Vehicle<T extends Engine> {

    @Inject
    T eng;
}
