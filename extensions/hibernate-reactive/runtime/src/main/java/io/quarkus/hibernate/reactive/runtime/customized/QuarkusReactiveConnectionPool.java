package io.quarkus.hibernate.reactive.runtime.customized;

import java.util.Map;

import org.hibernate.reactive.pool.impl.SqlClientPool;

import io.vertx.sqlclient.Pool;

@SuppressWarnings("serial")
public class QuarkusReactiveConnectionPool extends SqlClientPool {

    private final Pool pool;

    public QuarkusReactiveConnectionPool(Pool pool) {
        this.pool = pool;
    }

    protected Pool createPool(Map configurationValues) {
        return pool;
    }

    @Override
    public void stop() {
    }

}