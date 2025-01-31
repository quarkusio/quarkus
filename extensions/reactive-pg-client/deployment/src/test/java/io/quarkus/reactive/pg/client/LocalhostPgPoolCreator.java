package io.quarkus.reactive.pg.client;

import jakarta.inject.Singleton;

import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostPgPoolCreator implements PgPoolCreator {

    @Override
    public Pool create(Input input) {
        return Pool.pool(input.vertx(), input.pgConnectOptionsList().get(0).setHost("localhost").setPort(5431),
                input.poolOptions());
    }
}
