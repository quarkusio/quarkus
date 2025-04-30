package org.acme.services;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.stork.Stork;
import io.vertx.mutiny.core.Vertx;

@ApplicationScoped
public class Registration {

    @ConfigProperty(name = "red-service-port", defaultValue = "9000")
    int red;
    @ConfigProperty(name = "blue-service-port", defaultValue = "9001")
    int blue;

    /**
     * Register our two services using static list.
     *
     * Note: this method is called on a worker thread, and so it is allowed to block.
     */
    public void init(@Observes StartupEvent ev, Vertx vertx) {
        Stork.getInstance().getService("my-service").getServiceRegistrar().registerServiceInstance("my-service", "localhost",
                red);
        Stork.getInstance().getService("my-service").getServiceRegistrar().registerServiceInstance("my-service", "localhost",
                blue);
    }
}
