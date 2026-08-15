package io.quarkus.reactive.mssql.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.mssqlclient.MSSQLConnectOptions;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostMSSQLPoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return Pool.pool(input.vertx(),
                ((MSSQLConnectOptions) input.connectOptionsList().get(0)).setHost("localhost").setPort(1435),
                input.poolOptions());
    }
}
