package io.quarkus.reactive.pg.client.runtime;

import io.quarkus.reactive.datasource.runtime.ThreadLocalPool;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public class ThreadLocalPgPool extends ThreadLocalPool<PgPool> implements PgPool {

    private final PgConnectOptions pgConnectOptions;

    public ThreadLocalPgPool(Vertx vertx, PgConnectOptions pgConnectOptions, PoolOptions poolOptions) {
        super(vertx, poolOptions);
        this.pgConnectOptions = pgConnectOptions;
    }

    @Override
    protected PgPool createThreadLocalPool() {
        return PgPool.pool(vertx, pgConnectOptions, poolOptions);
    }
}
