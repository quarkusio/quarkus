package io.quarkus.reactive.mysql.client;

import jakarta.inject.Singleton;

import io.quarkus.reactive.datasource.PoolCreator;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostMySQLPoolCreator implements PoolCreator {

    @Override
    public Pool create(Input input) {
        return Pool.pool(input.vertx(),
                ((MySQLConnectOptions) input.connectOptionsList().get(0)).setHost("localhost").setPort(3308),
                input.poolOptions());
    }
}
