package io.quarkus.vertx.common.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

/**
 * Produces a configured Vert.x instance.
 * It also exposes the Vert.x event bus.
 */
@ApplicationScoped
public class VertxCommonProducer {

    private volatile Vertx vertx;

    void initialize(Vertx vertx) {
        this.vertx = vertx;
    }

    @Singleton
    @Produces
    public Vertx vertx() {
        return vertx;
    }

    @Singleton
    @Produces
    public EventBus eventbus() {
        return vertx.eventBus();
    }
}
