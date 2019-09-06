package io.quarkus.vertx.core.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.core.Vertx;

/**
 * Produces a configured Vert.x instance.
 */
@ApplicationScoped
public class VertxCoreProducer {

    private volatile Vertx vertx;

    void initialize(Vertx vertx) {
        this.vertx = vertx;
    }

    @Singleton
    @Produces
    public Vertx vertx() {
        return vertx;
    }
}
