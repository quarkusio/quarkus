package org.acme;

import io.quarkus.runtime.StartupEvent;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

@ApplicationScoped
public class HelloService {

    public void start(@Observes StartupEvent startupEvent) {

    }

    public String name() {
        return "Stuart";
    }

}
