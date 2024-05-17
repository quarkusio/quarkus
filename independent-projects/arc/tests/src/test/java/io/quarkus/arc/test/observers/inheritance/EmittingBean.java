package io.quarkus.arc.test.observers.inheritance;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class EmittingBean {

    public static final String VALUE = "my-event";

    @Inject
    Event<SimpleEvent> emitter;

    public void trigger() {
        emitter.fire(new SimpleEvent(VALUE));
    }
}
