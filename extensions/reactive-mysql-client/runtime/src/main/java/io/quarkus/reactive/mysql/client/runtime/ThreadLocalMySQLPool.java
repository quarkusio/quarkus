package io.quarkus.reactive.mysql.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.Vertx;
import io.vertx.mysqlclient.MySQLConnectOptions;
import io.vertx.mysqlclient.MySQLPool;
import io.vertx.sqlclient.PoolOptions;

public class ThreadLocalMySQLPool extends ThreadLocalPool<MySQLPool> implements MySQLPool {

    private final MySQLConnectOptions mySQLConnectOptions;

    public ThreadLocalMySQLPool(Vertx vertx, MySQLConnectOptions mySQLConnectOptions, PoolOptions poolOptions) {
        super(vertx, poolOptions);
        this.mySQLConnectOptions = mySQLConnectOptions;
    }

    @Override
    protected MySQLPool createThreadLocalPool() {
        return MySQLPool.pool(vertx, mySQLConnectOptions, poolOptions);
    }
}
