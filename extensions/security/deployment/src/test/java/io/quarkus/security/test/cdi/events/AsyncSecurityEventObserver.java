package io.quarkus.security.test.cdi.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.ObservesAsync;
import jakarta.inject.Singleton;

import io.quarkus.security.spi.runtime.SecurityEvent;

@Singleton
public class AsyncSecurityEventObserver {

    private final List<SecurityEvent> observerEvents = new CopyOnWriteArrayList<>();

    void logSecurityEvents(@ObservesAsync SecurityEvent securityEvent) {
        observerEvents.add(securityEvent);
    }

    List<SecurityEvent> getObserverEvents() {
        return observerEvents;
    }
}
