package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class StartupObserver {

    void onStart(@Observes StartupEvent event) {
        System.out.println("QUARKUS_FORK_STARTUP:" + ProcessHandle.current().pid());
    }
}
