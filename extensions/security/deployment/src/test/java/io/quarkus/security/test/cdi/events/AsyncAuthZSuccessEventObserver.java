package io.quarkus.security.test.cdi.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Singleton;

import io.quarkus.security.spi.runtime.AuthorizationSuccessEvent;

@Singleton
public class AsyncAuthZSuccessEventObserver {

    private final List<AuthorizationSuccessEvent> observerEvents = new CopyOnWriteArrayList<>();

    void logAuthZEvents(@ObservesAsync AuthorizationSuccessEvent authZEvent) {
        observerEvents.add(authZEvent);
    }

    List<AuthorizationSuccessEvent> getObserverEvents() {
        return observerEvents;
    }
}
