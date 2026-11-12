package org.acme.app;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class StartupObserver {

    static final String STARTED = "acme.app.started";

    void onStart(@Observes StartupEvent event) {
        System.out.println("Application started with greeting: " + System.getProperty("acme.ext.greeting"));
        System.setProperty(STARTED, "true");
    }
}
