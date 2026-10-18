package io.quarkus.reactive.pg.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostPgPoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return Pool.pool(input.vertx(), input.connectOptionsList().get(0).setHost("localhost").setPort(5431),
                input.poolOptions());
    }
}
