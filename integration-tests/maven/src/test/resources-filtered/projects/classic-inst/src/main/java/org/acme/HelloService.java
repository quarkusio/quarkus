package org.acme;

import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class HelloService {

    public void start(@Observes StartupEvent startupEvent) {

    }

    public String name() {
        return "Stuart";
    }

}
