package io.quarkus.redis.datasource.impl;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.api.ScanArgs;
import io.quarkus.redis.datasource.api.hash.HashCommands;
import io.quarkus.redis.datasource.api.hash.HashScanCursor;
import io.quarkus.redis.datasource.api.hash.ReactiveHashCommands;

public class BlockingHashCommandsImpl<K, F, V> implements HashCommands<K, F, V> {

    private final ReactiveHashCommands<K, F, V> reactive;
    private final Duration timeout;

    public BlockingHashCommandsImpl(ReactiveHashCommands<K, F, V> reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
    }

    @Override
    public int hdel(K key, F... fields) {
        return reactive.hdel(key, fields)
                .await().atMost(timeout);
    }

    @Override
    public boolean hexists(K key, F field) {
        return reactive.hexists(key, field)
                .await().atMost(timeout);
    }

    @Override
    public V hget(K key, F field) {
        return reactive.hget(key, field)
                .await().atMost(timeout);
    }

    @Override
    public long hincrby(K key, F field, long amount) {
        return reactive.hincrby(key, field, amount)
                .await().atMost(timeout);
    }

    @Override
    public double hincrbyfloat(K key, F field, double amount) {
        return reactive.hincrbyfloat(key, field, amount)
                .await().atMost(timeout);
    }

    @Override
    public Map<F, V> hgetall(K key) {
        return reactive.hgetall(key)
                .await().atMost(timeout);
    }

    @Override
    public List<F> hkeys(K key) {
        return reactive.hkeys(key).await().atMost(timeout);
    }

    @Override
    public long hlen(K key) {
        return reactive.hlen(key)
                .await().atMost(timeout);
    }

    @Override
    public Map<F, V> hmget(K key, F... fields) {
        return reactive.hmget(key, fields)
                .await().atMost(timeout);
    }

    @Override
    public void hmset(K key, Map<F, V> map) {
        reactive.hmset(key, map)
                .await().atMost(timeout);
    }

    @Override
    public F hrandfield(K key) {
        return reactive.hrandfield(key)
                .await().atMost(timeout);
    }

    @Override
    public List<F> hrandfield(K key, long count) {
        return reactive.hrandfield(key, count)
                .await().atMost(timeout);
    }

    @Override
    public Map<F, V> hrandfieldWithValues(K key, long count) {
        return reactive.hrandfieldWithValues(key, count)
                .await().atMost(timeout);
    }

    @Override
    public HashScanCursor<F, V> hscan(K key) {
        return new HashScanBlockingCursorImpl<>(reactive.hscan(key), timeout);
    }

    @Override
    public HashScanCursor<F, V> hscan(K key, ScanArgs scanArgs) {
        return new HashScanBlockingCursorImpl<>(reactive.hscan(key, scanArgs), timeout);
    }

    @Override
    public boolean hset(K key, F field, V value) {
        return reactive.hset(key, field, value)
                .await().atMost(timeout);
    }

    @Override
    public long hset(K key, Map<F, V> map) {
        return reactive.hset(key, map)
                .await().atMost(timeout);
    }

    @Override
    public boolean hsetnx(K key, F field, V value) {
        return reactive.hsetnx(key, field, value)
                .await().atMost(timeout);
    }

    @Override
    public long hstrlen(K key, F field) {
        return reactive.hstrlen(key, field)
                .await().atMost(timeout);
    }

    @Override
    public List<V> hvals(K key) {
        return reactive.hvals(key)
                .await().atMost(timeout);
    }
}
