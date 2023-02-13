package io.quarkus.reactive.mysql.client;

import jakarta.inject.Singleton;

import io.vertx.mysqlclient.MySQLPool;

@Singleton
public class LocalhostMySQLPoolCreator implements MySQLPoolCreator {

    @Override
    public MySQLPool create(Input input) {
        return MySQLPool.pool(input.vertx(), input.mySQLConnectOptions().setHost("localhost").setPort(3308),
                input.poolOptions());
    }
}
