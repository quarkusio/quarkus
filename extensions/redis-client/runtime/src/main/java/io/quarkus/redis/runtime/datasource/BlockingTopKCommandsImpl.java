package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.quarkus.redis.datasource.topk.TopKCommands;

public class BlockingTopKCommandsImpl<K, V> extends AbstractRedisCommandGroup implements TopKCommands<K, V> {

    private final ReactiveTopKCommands<K, V> reactive;

    public BlockingTopKCommandsImpl(RedisDataSource ds, ReactiveTopKCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public Optional<V> topkAdd(K key, V item) {
        return Optional.ofNullable(reactive.topkAdd(key, item).await().atMost(timeout));
    }

    @Override
    public List<V> topkAdd(K key, V... items) {
        return reactive.topkAdd(key, items).await().atMost(timeout);
    }

    @Override
    public Optional<V> topkIncrBy(K key, V item, int increment) {
        return Optional.ofNullable(reactive.topkIncrBy(key, item, increment).await().atMost(timeout));
    }

    @Override
    public Map<V, V> topkIncrBy(K key, Map<V, Integer> couples) {
        return reactive.topkIncrBy(key, couples).await().atMost(timeout);
    }

    @Override
    public List<V> topkList(K key) {
        return reactive.topkList(key).await().atMost(timeout);
    }

    @Override
    public Map<V, Integer> topkListWithCount(K key) {
        return reactive.topkListWithCount(key).await().atMost(timeout);
    }

    @Override
    public boolean topkQuery(K key, V item) {
        return reactive.topkQuery(key, item).await().atMost(timeout);
    }

    @Override
    public List<Boolean> topkQuery(K key, V... items) {
        return reactive.topkQuery(key, items).await().atMost(timeout);
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
