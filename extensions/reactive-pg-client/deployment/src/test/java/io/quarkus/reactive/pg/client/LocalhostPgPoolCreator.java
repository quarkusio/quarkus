package io.quarkus.reactive.pg.client;

import jakarta.inject.Singleton;

import io.vertx.pgclient.PgPool;

@Singleton
public class LocalhostPgPoolCreator implements PgPoolCreator {

    @Override
    public PgPool create(Input input) {
        return PgPool.pool(input.vertx(), input.pgConnectOptionsList().get(0).setHost("localhost").setPort(5431),
                input.poolOptions());
    }
}
