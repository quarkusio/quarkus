package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.TransactionalRedisCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class AbstractTransactionalRedisCommandGroup implements TransactionalRedisCommands {

    protected final TransactionalRedisDataSource ds;
    protected final Duration timeout;

    public AbstractTransactionalRedisCommandGroup(TransactionalRedisDataSource ds, Duration timeout) {
        this.ds = ds;
        this.timeout = timeout;
    }

    @Override
    public TransactionalRedisDataSource getDataSource() {
        return ds;
    }
}
