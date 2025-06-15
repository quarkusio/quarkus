package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.BloomCommands;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;

public class BlockingBloomCommandsImpl<K, V> extends AbstractRedisCommandGroup implements BloomCommands<K, V> {

    private final ReactiveBloomCommands<K, V> reactive;

    public BlockingBloomCommandsImpl(RedisDataSource ds, ReactiveBloomCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public boolean bfadd(K key, V value) {
        return reactive.bfadd(key, value).await().atMost(timeout);
    }

    @Override
    public boolean bfexists(K key, V value) {
        return reactive.bfexists(key, value).await().atMost(timeout);
    }

    @Override
    public List<Boolean> bfmadd(K key, V... values) {
        return reactive.bfmadd(key, values).await().atMost(timeout);
    }

    @Override
    public List<Boolean> bfmexists(K key, V... values) {
        return reactive.bfmexists(key, values).await().atMost(timeout);
    }

    @Override
    public void bfreserve(K key, double errorRate, long capacity) {
        reactive.bfreserve(key, errorRate, capacity).await().atMost(timeout);
    }

    @Override
    public void bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        reactive.bfreserve(key, errorRate, capacity, args).await().atMost(timeout);
    }

    @Override
    public List<Boolean> bfinsert(K key, BfInsertArgs args, V... values) {
        return reactive.bfinsert(key, args, values).await().atMost(timeout);
    }
}
