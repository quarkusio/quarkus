package io.quarkus.vertx.core.runtime;

import java.util.function.Supplier;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.core.Vertx;

/**
 * Produces a configured Vert.x instance.
 */
@ApplicationScoped
public class VertxCoreProducer {

    private volatile Supplier<Vertx> vertx;

    void initialize(Supplier<Vertx> vertx) {
        this.vertx = vertx;
    }

    @Singleton
    @Produces
    public Vertx vertx() {
        return vertx.get();
    }
}
