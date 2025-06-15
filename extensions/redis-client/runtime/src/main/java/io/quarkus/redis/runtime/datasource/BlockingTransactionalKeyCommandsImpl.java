package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.keys.TransactionalKeyCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalKeyCommandsImpl<K> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalKeyCommands<K> {

    private final ReactiveTransactionalKeyCommands<K> reactive;

    public BlockingTransactionalKeyCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalKeyCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void copy(K source, K destination) {
        this.reactive.copy(source, destination).await().atMost(this.timeout);
    }

    @Override
    public void copy(K source, K destination, CopyArgs copyArgs) {
        this.reactive.copy(source, destination, copyArgs).await().atMost(this.timeout);
    }

    @Override
    public void del(K... keys) {
        this.reactive.del(keys).await().atMost(this.timeout);
    }

    @Override
    public void dump(K key) {
        this.reactive.dump(key).await().atMost(this.timeout);
    }

    @Override
    public void exists(K key) {
        this.reactive.exists(key).await().atMost(this.timeout);
    }

    @Override
    public void exists(K... keys) {
        this.reactive.exists(keys).await().atMost(this.timeout);
    }

    @Override
    public void expire(K key, long seconds, ExpireArgs expireArgs) {
        this.reactive.expire(key, seconds, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void expire(K key, Duration duration, ExpireArgs expireArgs) {
        this.reactive.expire(key, duration, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void expire(K key, long seconds) {
        this.reactive.expire(key, seconds).await().atMost(this.timeout);
    }

    @Override
    public void expire(K key, Duration duration) {
        this.reactive.expire(key, duration).await().atMost(this.timeout);
    }

    @Override
    public void expireat(K key, long timestamp) {
        this.reactive.expireat(key, timestamp).await().atMost(this.timeout);
    }

    @Override
    public void expireat(K key, Instant timestamp) {
        this.reactive.expireat(key, timestamp).await().atMost(this.timeout);
    }

    @Override
    public void expireat(K key, long timestamp, ExpireArgs expireArgs) {
        this.reactive.expireat(key, timestamp, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        this.reactive.expireat(key, timestamp, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void expiretime(K key) {
        this.reactive.expiretime(key).await().atMost(this.timeout);
    }

    @Override
    public void keys(String pattern) {
        this.reactive.keys(pattern).await().atMost(this.timeout);
    }

    @Override
    public void move(K key, long db) {
        this.reactive.move(key, db).await().atMost(this.timeout);
    }

    @Override
    public void persist(K key) {
        this.reactive.persist(key).await().atMost(this.timeout);
    }

    @Override
    public void pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        this.reactive.pexpire(key, duration, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void pexpire(K key, long ms) {
        this.reactive.pexpire(key, ms).await().atMost(this.timeout);
    }

    @Override
    public void pexpire(K key, Duration duration) {
        this.reactive.pexpire(key, duration).await().atMost(this.timeout);
    }

    @Override
    public void pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        this.reactive.pexpire(key, milliseconds, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void pexpireat(K key, long timestamp) {
        this.reactive.pexpireat(key, timestamp).await().atMost(this.timeout);
    }

    @Override
    public void pexpireat(K key, Instant timestamp) {
        this.reactive.pexpireat(key, timestamp).await().atMost(this.timeout);
    }

    @Override
    public void pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        this.reactive.pexpireat(key, timestamp, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        this.reactive.pexpireat(key, timestamp, expireArgs).await().atMost(this.timeout);
    }

    @Override
    public void pexpiretime(K key) {
        this.reactive.pexpiretime(key).await().atMost(this.timeout);
    }

    @Override
    public void pttl(K key) throws RedisKeyNotFoundException {
        this.reactive.pttl(key).await().atMost(this.timeout);
    }

    @Override
    public void randomkey() {
        this.reactive.randomkey().await().atMost(this.timeout);
    }

    @Override
    public void rename(K key, K newkey) {
        this.reactive.rename(key, newkey).await().atMost(this.timeout);
    }

    @Override
    public void renamenx(K key, K newkey) {
        this.reactive.renamenx(key, newkey).await().atMost(this.timeout);
    }

    @Override
    public void touch(K... keys) {
        this.reactive.touch(keys).await().atMost(this.timeout);
    }

    @Override
    public void ttl(K key) throws RedisKeyNotFoundException {
        this.reactive.ttl(key).await().atMost(this.timeout);
    }

    @Override
    public void type(K key) {
        this.reactive.type(key).await().atMost(this.timeout);
    }

    @Override
    public void unlink(K... keys) {
        this.reactive.unlink(keys).await().atMost(this.timeout);
    }
}
