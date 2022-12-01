package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.RedisCommands;
import io.quarkus.redis.datasource.RedisDataSource;

public class AbstractRedisCommandGroup implements RedisCommands {

    protected final RedisDataSource ds;
    protected final Duration timeout;

    public AbstractRedisCommandGroup(RedisDataSource ds, Duration timeout) {
        this.ds = ds;
        this.timeout = timeout;
    }

    @Override
    public RedisDataSource getDataSource() {
        return ds;
    }
}
