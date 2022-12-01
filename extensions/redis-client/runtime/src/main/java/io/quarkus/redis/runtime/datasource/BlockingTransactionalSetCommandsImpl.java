package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.set.TransactionalSetCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalSetCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalSetCommands<K, V> {

    private final ReactiveTransactionalSetCommands<K, V> reactive;

    public BlockingTransactionalSetCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalSetCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void sadd(K key, V... values) {
        this.reactive.sadd(key, values).await().atMost(this.timeout);
    }

    @Override
    public void scard(K key) {
        this.reactive.scard(key).await().atMost(this.timeout);
    }

    @Override
    public void sdiff(K... keys) {
        this.reactive.sdiff(keys).await().atMost(this.timeout);
    }

    @Override
    public void sdiffstore(K destination, K... keys) {
        this.reactive.sdiffstore(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void sinter(K... keys) {
        this.reactive.sinter(keys).await().atMost(this.timeout);
    }

    @Override
    public void sintercard(K... keys) {
        this.reactive.sintercard(keys).await().atMost(this.timeout);
    }

    @Override
    public void sintercard(int limit, K... keys) {
        this.reactive.sintercard(limit, keys).await().atMost(this.timeout);
    }

    @Override
    public void sinterstore(K destination, K... keys) {
        this.reactive.sinterstore(destination, keys).await().atMost(this.timeout);
    }

    @Override
    public void sismember(K key, V member) {
        this.reactive.sismember(key, member).await().atMost(this.timeout);
    }

    @Override
    public void smembers(K key) {
        this.reactive.smembers(key).await().atMost(this.timeout);
    }

    @Override
    public void smismember(K key, V... members) {
        this.reactive.smismember(key, members).await().atMost(this.timeout);
    }

    @Override
    public void smove(K source, K destination, V member) {
        this.reactive.smove(source, destination, member).await().atMost(this.timeout);
    }

    @Override
    public void spop(K key) {
        this.reactive.spop(key).await().atMost(this.timeout);
    }

    @Override
    public void spop(K key, int count) {
        this.reactive.spop(key, count).await().atMost(this.timeout);
    }

    @Override
    public void srandmember(K key) {
        this.reactive.srandmember(key).await().atMost(this.timeout);
    }

    @Override
    public void srandmember(K key, int count) {
        this.reactive.srandmember(key, count).await().atMost(this.timeout);
    }

    @Override
    public void srem(K key, V... members) {
        this.reactive.srem(key, members).await().atMost(this.timeout);
    }

    @Override
    public void sunion(K... keys) {
        this.reactive.sunion(keys).await().atMost(this.timeout);
    }

    @Override
    public void sunionstore(K destination, K... keys) {
        this.reactive.sunionstore(destination, keys).await().atMost(this.timeout);
    }
}
