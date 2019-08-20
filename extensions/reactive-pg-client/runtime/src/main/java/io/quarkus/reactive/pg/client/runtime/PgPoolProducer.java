package io.quarkus.reactive.pg.client.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.vertx.pgclient.PgPool;

@ApplicationScoped
public class PgPoolProducer {

    private volatile PgPool pgPool;
    private volatile io.vertx.axle.pgclient.PgPool axlePgPool;
    private volatile io.vertx.reactivex.pgclient.PgPool rxPgPool;

    void initialize(PgPool pgPool) {
        this.pgPool = pgPool;
        this.axlePgPool = io.vertx.axle.pgclient.PgPool.newInstance(pgPool);
        this.rxPgPool = io.vertx.reactivex.pgclient.PgPool.newInstance(pgPool);
    }

    @Singleton
    @Produces
    public PgPool pgPool() {
        return pgPool;
    }

    @Singleton
    @Produces
    public io.vertx.axle.pgclient.PgPool axlePgPool() {
        return axlePgPool;
    }

    @Singleton
    @Produces
    public io.vertx.reactivex.pgclient.PgPool rxPgPool() {
        return rxPgPool;
    }
}
