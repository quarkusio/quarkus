package io.quarkus.reactive.oracle.client;

import jakarta.inject.Singleton;

import io.vertx.oracleclient.OraclePool;

@Singleton
public class LocalhostOraclePoolCreator implements OraclePoolCreator {

    @Override
    public OraclePool create(Input input) {
        return OraclePool.pool(input.vertx(), input.oracleConnectOptions().setHost("localhost").setPort(1521),
                input.poolOptions());
    }
}
