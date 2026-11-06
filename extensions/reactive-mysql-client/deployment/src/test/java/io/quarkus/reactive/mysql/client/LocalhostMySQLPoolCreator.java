package io.quarkus.reactive.mysql.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.mysqlclient.MySQLBuilder;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostMySQLPoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return MySQLBuilder.pool()
                .with(input.poolOptions())
                .connectingTo(() -> input.dynamicConnectOptionsList().get()
                        .map(options -> options.setHost("localhost").setPort(3308)))
                .using(input.vertx())
                .build();
    }
}
