package io.quarkus.redis.datasource;

/**
 * Interface implemented by <em>imperative</em> Redis command groups.
 */
public interface RedisCommands {

    /**
     * @return the data source.
     */
    RedisDataSource getDataSource();
}
