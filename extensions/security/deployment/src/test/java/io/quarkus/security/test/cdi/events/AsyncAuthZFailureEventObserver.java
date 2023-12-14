package io.quarkus.security.test.cdi.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Singleton;

import io.quarkus.security.spi.runtime.AuthorizationFailureEvent;

@Singleton
public class AsyncAuthZFailureEventObserver {

    private final List<AuthorizationFailureEvent> observerEvents = new CopyOnWriteArrayList<>();

    void logAuthZEvents(@ObservesAsync AuthorizationFailureEvent authZEvent) {
        observerEvents.add(authZEvent);
    }

    List<AuthorizationFailureEvent> getObserverEvents() {
        return observerEvents;
    }
}
