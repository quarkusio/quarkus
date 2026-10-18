package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.topk.TransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalTopKCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalTopKCommands<K, V> {

    private final ReactiveTransactionalTopKCommands<K, V> reactive;

    public BlockingTransactionalTopKCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalTopKCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void topkAdd(K key, V item) {
        reactive.topkAdd(key, item).await().atMost(timeout);
    }

    @Override
    public void topkAdd(K key, V... items) {
        reactive.topkAdd(key, items).await().atMost(timeout);
    }

    @Override
    public void topkIncrBy(K key, V item, int increment) {
        reactive.topkIncrBy(key, item, increment).await().atMost(timeout);
    }

    @Override
    public void topkIncrBy(K key, Map<V, Integer> couples) {
        reactive.topkIncrBy(key, couples).await().atMost(timeout);
    }

    @Override
    public void topkList(K key) {
        reactive.topkList(key).await().atMost(timeout);
    }

    @Override
    public void topkListWithCount(K key) {
        reactive.topkListWithCount(key).await().atMost(timeout);
    }

    @Override
    public void topkQuery(K key, V item) {
        reactive.topkQuery(key, item).await().atMost(timeout);
    }

    @Override
    public void topkQuery(K key, V... items) {
        reactive.topkQuery(key, items).await().atMost(timeout);
    }

    @Override
    public void topkReserve(K key, int topk) {
        reactive.topkReserve(key, topk).await().atMost(timeout);
    }

    @Override
    public void topkReserve(K key, int topk, int width, int depth, double decay) {
        reactive.topkReserve(key, topk, width, depth, decay).await().atMost(timeout);
    }
}
