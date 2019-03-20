package io.quarkus.vertx.runtime;

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
public class VertxProducer {

    private volatile Vertx vertx;
    private volatile io.vertx.axle.core.Vertx axleVertx;
    private volatile io.vertx.reactivex.core.Vertx rxVertx;

    void initialize(Vertx vertx) {
        this.vertx = vertx;
        this.axleVertx = io.vertx.axle.core.Vertx.newInstance(vertx);
        this.rxVertx = io.vertx.reactivex.core.Vertx.newInstance(vertx);
    }

    @Singleton
    @Produces
    public Vertx vertx() {
        return vertx;
    }

    @Singleton
    @Produces
    public io.vertx.axle.core.Vertx axle() {
        return axleVertx;
    }

    @Singleton
    @Produces
    public io.vertx.reactivex.core.Vertx rx() {
        return rxVertx;
    }

    @Singleton
    @Produces
    public EventBus eventbus() {
        return vertx.eventBus();
    }

    @Singleton
    @Produces
    public io.vertx.axle.core.eventbus.EventBus axleEventbus() {
        return axleVertx.eventBus();
    }

    @Singleton
    @Produces
    public synchronized io.vertx.reactivex.core.eventbus.EventBus rxRventbus() {
        return rxVertx.eventBus();
    }

}
