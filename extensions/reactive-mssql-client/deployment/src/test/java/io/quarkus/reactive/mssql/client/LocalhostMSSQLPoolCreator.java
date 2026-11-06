package io.quarkus.reactive.mssql.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.mssqlclient.MSSQLBuilder;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostMSSQLPoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return MSSQLBuilder.pool()
                .with(input.poolOptions())
                .connectingTo(() -> input.dynamicConnectOptionsList().get()
                        .map(options -> options.setHost("localhost").setPort(1435)))
                .using(input.vertx())
                .build();
    }
}
