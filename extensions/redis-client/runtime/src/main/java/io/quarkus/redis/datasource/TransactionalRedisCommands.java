package io.quarkus.redis.datasource;

import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

/**
 * Interface implemented by <em>transactional and imperative</em> Redis command groups.
 */
public interface TransactionalRedisCommands {

    /**
     * @return the data source.
     */
    TransactionalRedisDataSource getDataSource();
}
