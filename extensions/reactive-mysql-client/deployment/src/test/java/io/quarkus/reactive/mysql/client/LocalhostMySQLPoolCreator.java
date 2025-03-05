package io.quarkus.reactive.mysql.client;

import jakarta.inject.Singleton;

import io.vertx.sqlclient.Pool;

@Singleton
public class LocalhostMySQLPoolCreator implements MySQLPoolCreator {

    @Override
    public Pool create(Input input) {
        return Pool.pool(input.vertx(), input.mySQLConnectOptionsList().get(0).setHost("localhost").setPort(3308),
                input.poolOptions());
    }
}
