package io.quarkus.redis.datasource;

import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;

/**
 * Interface implemented by <em>transactional and reactive</em> Redis command groups.
 */
public interface ReactiveTransactionalRedisCommands {

    /**
     * @return the data source.
     */
    ReactiveTransactionalRedisDataSource getDataSource();
}
