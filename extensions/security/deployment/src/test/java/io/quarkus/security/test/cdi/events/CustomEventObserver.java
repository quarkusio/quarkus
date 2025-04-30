package io.quarkus.security.test.cdi.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Singleton;

@Singleton
public class CustomEventObserver {

    private final List<CustomSecurityEvent> observerEvents = new CopyOnWriteArrayList<>();

    void logAuthZEvents(@Observes CustomSecurityEvent authZEvent) {
        observerEvents.add(authZEvent);
    }

    List<CustomSecurityEvent> getObserverEvents() {
        return observerEvents;
    }
}
