package io.quarkus.reactive.oracle.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.oracleclient.OracleConnectOptions;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostOraclePoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        OracleConnectOptions oracleConnectOptions = (OracleConnectOptions) input.connectOptionsList().get(0);
        return Pool.pool(input.vertx(), oracleConnectOptions.setHost("localhost").setPort(1521),
                input.poolOptions());
    }
}
