package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.hash.TransactionalHashCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalHashCommandsImpl<K, F, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalHashCommands<K, F, V> {

    private final ReactiveTransactionalHashCommands<K, F, V> reactive;

    public BlockingTransactionalHashCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalHashCommands<K, F, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void hdel(K key, F... fields) {
        this.reactive.hdel(key, fields).await().atMost(this.timeout);
    }

    @Override
    public void hexists(K key, F field) {
        this.reactive.hexists(key, field).await().atMost(this.timeout);
    }

    @Override
    public void hget(K key, F field) {
        this.reactive.hget(key, field).await().atMost(this.timeout);
    }

    @Override
    public void hincrby(K key, F field, long amount) {
        this.reactive.hincrby(key, field, amount).await().atMost(this.timeout);
    }

    @Override
    public void hincrbyfloat(K key, F field, double amount) {
        this.reactive.hincrbyfloat(key, field, amount).await().atMost(this.timeout);
    }

    @Override
    public void hgetall(K key) {
        this.reactive.hgetall(key).await().atMost(this.timeout);
    }

    @Override
    public void hkeys(K key) {
        this.reactive.hkeys(key).await().atMost(this.timeout);
    }

    @Override
    public void hlen(K key) {
        this.reactive.hlen(key).await().atMost(this.timeout);
    }

    @Override
    public void hmget(K key, F... fields) {
        this.reactive.hmget(key, fields).await().atMost(this.timeout);
    }

    @Deprecated
    @Override
    public void hmset(K key, Map<F, V> map) {
        this.reactive.hmset(key, map).await().atMost(this.timeout);
    }

    @Override
    public void hrandfield(K key) {
        this.reactive.hrandfield(key).await().atMost(this.timeout);
    }

    @Override
    public void hrandfield(K key, long count) {
        this.reactive.hrandfield(key, count).await().atMost(this.timeout);
    }

    @Override
    public void hrandfieldWithValues(K key, long count) {
        this.reactive.hrandfieldWithValues(key, count).await().atMost(this.timeout);
    }

    @Override
    public void hset(K key, F field, V value) {
        this.reactive.hset(key, field, value).await().atMost(this.timeout);
    }

    @Override
    public void hset(K key, Map<F, V> map) {
        this.reactive.hset(key, map).await().atMost(this.timeout);
    }

    @Override
    public void hsetnx(K key, F field, V value) {
        this.reactive.hsetnx(key, field, value).await().atMost(this.timeout);
    }

    @Override
    public void hstrlen(K key, F field) {
        this.reactive.hstrlen(key, field).await().atMost(this.timeout);
    }

    @Override
    public void hvals(K key) {
        this.reactive.hvals(key).await().atMost(this.timeout);
    }
}
