package io.quarkus.reactive.oracle.client;

import jakarta.inject.Singleton;

import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostOraclePoolCreator implements OraclePoolCreator {

    @Override
    public Pool create(Input input) {
        return Pool.pool(input.vertx(), input.oracleConnectOptions().setHost("localhost").setPort(1521),
                input.poolOptions());
    }
}
