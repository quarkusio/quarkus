package io.quarkus.it.mockbean;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

@Singleton
public class FooBarObserver {

    void onBar(@Observes Foo.Bar bar) {
        bar.getNames().add("baz");
    }

}
