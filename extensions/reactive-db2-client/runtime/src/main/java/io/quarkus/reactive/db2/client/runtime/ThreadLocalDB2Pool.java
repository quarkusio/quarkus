package io.quarkus.reactive.db2.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.Vertx;
import io.vertx.db2client.DB2ConnectOptions;
import io.vertx.db2client.DB2Pool;
import io.vertx.sqlclient.PoolOptions;

public class ThreadLocalDB2Pool extends ThreadLocalPool<DB2Pool> implements DB2Pool {

    private final DB2ConnectOptions db2ConnectOptions;

    public ThreadLocalDB2Pool(Vertx vertx, DB2ConnectOptions db2ConnectOptions, PoolOptions poolOptions) {
        super(vertx, poolOptions);
        this.db2ConnectOptions = db2ConnectOptions;
    }

    @Override
    protected DB2Pool createThreadLocalPool() {
        return DB2Pool.pool(vertx, db2ConnectOptions, poolOptions);
    }
}
