package io.quarkus.grpc.auth;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.security.spi.runtime.SecurityEvent;

@ApplicationScoped
public class SecurityEventObserver {

    private final List<SecurityEvent> storage = new CopyOnWriteArrayList<>();

    void observe(@Observes SecurityEvent event) {
        storage.add(event);
    }

    List<SecurityEvent> getStorage() {
        return storage;
    }
}
