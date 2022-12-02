package io.quarkus.reactive.datasource.runtime;

import io.vertx.sqlclient.Pool;

/**
 * Having an interface here is useful to mimic the
 * same patterns as DB2Pool, MySQLPool, PgPool, etc..
 */
public interface TestPoolInterface extends Pool {
    boolean isClosed();
}
