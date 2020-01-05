package io.quarkus.vertx.runtime;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

/**
 * Expose the Vert.x event bus and produces Axle and Rx Vert.x instances.
 * <p>
 * The original Vert.x instance is coming from the core artifact.
 */
@ApplicationScoped
public class VertxProducer {

    @Inject
    Vertx vertx;

    private io.vertx.axle.core.Vertx axleVertx;
    private io.vertx.reactivex.core.Vertx rxVertx;
    private io.vertx.mutiny.core.Vertx mutinyVertx;

    @PostConstruct
    public void initialize() {
        this.axleVertx = io.vertx.axle.core.Vertx.newInstance(vertx);
        this.rxVertx = io.vertx.reactivex.core.Vertx.newInstance(vertx);
        this.mutinyVertx = io.vertx.mutiny.core.Vertx.newInstance(vertx);
    }

    @Singleton
    @Produces
    public EventBus eventbus() {
        return vertx.eventBus();
    }

    @Singleton
    @Produces
    public io.vertx.axle.core.Vertx axle() {
        return axleVertx;
    }

    @Singleton
    @Produces
    public io.vertx.mutiny.core.Vertx mutiny() {
        return mutinyVertx;
    }

    @Singleton
    @Produces
    public io.vertx.reactivex.core.Vertx rx() {
        return rxVertx;
    }

    @Singleton
    @Produces
    public io.vertx.axle.core.eventbus.EventBus axleEventbus() {
        return axleVertx.eventBus();
    }

    @Singleton
    @Produces
    public io.vertx.mutiny.core.eventbus.EventBus mutinyEventbus() {
        return mutinyVertx.eventBus();
    }

    @Singleton
    @Produces
    public synchronized io.vertx.reactivex.core.eventbus.EventBus rxEventbus() {
        return rxVertx.eventBus();
    }
}
