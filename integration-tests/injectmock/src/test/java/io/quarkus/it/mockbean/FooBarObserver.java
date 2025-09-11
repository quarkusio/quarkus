package io.quarkus.it.mockbean;

import java.util.ArrayList;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class FooBarObserver {

    @Inject
    Event<Foo.Bar> event;

    Foo.Bar fireBar() {
        Foo.Bar bar = new Foo.Bar(new ArrayList<>());
        event.fire(bar);
        return bar;
    }

    void onBar(@Observes Foo.Bar bar) {
        bar.getNames().add("baz");
    }

}
