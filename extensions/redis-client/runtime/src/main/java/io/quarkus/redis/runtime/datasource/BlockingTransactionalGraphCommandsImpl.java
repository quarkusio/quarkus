package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.graph.ReactiveTransactionalGraphCommands;
import io.quarkus.redis.datasource.graph.TransactionalGraphCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalGraphCommandsImpl<K> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalGraphCommands<K> {

    private final ReactiveTransactionalGraphCommands<K> reactive;

    public BlockingTransactionalGraphCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalGraphCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void graphDelete(K key) {
        reactive.graphDelete(key).await().atMost(timeout);
    }

    @Override
    public void graphExplain(K key, String query) {
        reactive.graphExplain(key, query).await().atMost(timeout);
    }

    @Override
    public void graphList() {
        reactive.graphList().await().atMost(timeout);
    }

    @Override
    public void graphQuery(K key, String query) {
        reactive.graphQuery(key, query).await().atMost(timeout);
    }

    @Override
    public void graphQuery(K key, String query, Duration timeout) {
        reactive.graphQuery(key, query, timeout).await().atMost(timeout);
    }
}
