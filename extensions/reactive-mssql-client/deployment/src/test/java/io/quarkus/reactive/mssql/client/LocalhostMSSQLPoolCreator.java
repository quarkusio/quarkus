package io.quarkus.reactive.mssql.client;

import jakarta.inject.Singleton;

import io.vertx.mssqlclient.MSSQLPool;

@Singleton
public class LocalhostMSSQLPoolCreator implements MSSQLPoolCreator {

    @Override
    public MSSQLPool create(Input input) {
        return MSSQLPool.pool(input.vertx(), input.msSQLConnectOptionsList().get(0).setHost("localhost").setPort(1435),
                input.poolOptions());
    }
}
