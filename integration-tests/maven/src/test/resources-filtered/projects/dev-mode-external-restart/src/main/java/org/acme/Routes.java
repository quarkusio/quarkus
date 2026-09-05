package org.acme;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.spi.Connector;

import io.quarkus.arc.Arc;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.vertx.ext.web.Router;

@ApplicationScoped
public class Routes {

    @Inject
    @Connector("smallrye-in-memory")
    InMemoryConnector connector;

    void routes(@Observes Router router) {
        router.route("/greeting")
                .handler(rc -> rc.response().end(Arc.requireContainer().instance(Greeting.class).get().message()));
        // a message on an incoming channel makes the messaging extension scan for changes and restart the
        // application from its own thread, like a Kafka message would
        router.route("/trigger").handler(rc -> {
            connector.source("changes").send("scan");
            rc.response().end("triggered");
        });
    }

    @Incoming("changes")
    void onChange(String message) {
    }
}
