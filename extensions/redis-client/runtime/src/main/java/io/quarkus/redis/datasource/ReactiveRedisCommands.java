package io.quarkus.redis.datasource;

/**
 * Interface implemented by <em>reactive</em> Redis command groups.
 */
public interface ReactiveRedisCommands {

    /**
     * @return the data source.
     */
    ReactiveRedisDataSource getDataSource();
}
