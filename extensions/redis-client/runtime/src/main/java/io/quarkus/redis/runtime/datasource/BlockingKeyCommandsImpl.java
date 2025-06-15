package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.KeyCommands;
import io.quarkus.redis.datasource.keys.KeyScanArgs;
import io.quarkus.redis.datasource.keys.KeyScanCursor;
import io.quarkus.redis.datasource.keys.ReactiveKeyCommands;
import io.quarkus.redis.datasource.keys.RedisValueType;

public class BlockingKeyCommandsImpl<K> extends AbstractRedisCommandGroup implements KeyCommands<K> {

    private final ReactiveKeyCommands<K> reactive;

    public BlockingKeyCommandsImpl(RedisDataSource ds, ReactiveKeyCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public boolean copy(K source, K destination) {
        return reactive.copy(source, destination).await().atMost(timeout);
    }

    @Override
    public boolean copy(K source, K destination, CopyArgs copyArgs) {
        return reactive.copy(source, destination, copyArgs).await().atMost(timeout);
    }

    @Override
    public int del(K... keys) {
        return reactive.del(keys).await().atMost(timeout);
    }

    @Override
    public String dump(K key) {
        return reactive.dump(key).await().atMost(timeout);
    }

    @Override
    public boolean exists(K key) {
        return reactive.exists(key).await().atMost(timeout);
    }

    @Override
    public int exists(K... keys) {
        return reactive.exists(keys).await().atMost(timeout);
    }

    @Override
    public boolean expire(K key, long seconds, ExpireArgs expireArgs) {
        return reactive.expire(key, seconds, expireArgs).await().atMost(timeout);
    }

    @Override
    public boolean expire(K key, Duration duration, ExpireArgs expireArgs) {
        return reactive.expire(key, duration, expireArgs).await().atMost(timeout);
    }

    @Override
    public boolean expire(K key, long seconds) {
        return reactive.expire(key, seconds).await().atMost(timeout);
    }

    @Override
    public boolean expire(K key, Duration duration) {
        return reactive.expire(key, duration).await().atMost(timeout);
    }

    @Override
    public boolean expireat(K key, long timestamp) {
        return reactive.expireat(key, timestamp).await().atMost(timeout);
    }

    @Override
    public boolean expireat(K key, Instant timestamp) {
        return reactive.expireat(key, timestamp).await().atMost(timeout);
    }

    @Override
    public boolean expireat(K key, long timestamp, ExpireArgs expireArgs) {
        return reactive.expireat(key, timestamp, expireArgs).await().atMost(timeout);
    }

    @Override
    public boolean expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return reactive.expireat(key, timestamp, expireArgs).await().atMost(timeout);
    }

    @Override
    public long expiretime(K key) {
        return reactive.expiretime(key).await().atMost(timeout);
    }

    @Override
    public List<K> keys(String pattern) {
        return reactive.keys(pattern).await().atMost(timeout);
    }

    @Override
    public boolean move(K key, long db) {
        return reactive.move(key, db).await().atMost(timeout);
    }

    @Override
    public boolean persist(K key) {
        return reactive.persist(key).await().atMost(timeout);
    }

    @Override
    public boolean pexpire(K key, long seconds, ExpireArgs pexpireArgs) {
        return reactive.pexpire(key, seconds, pexpireArgs).await().atMost(timeout);
    }

    @Override
    public boolean pexpire(K key, Duration duration, ExpireArgs pexpireArgs) {
        return reactive.pexpire(key, duration, pexpireArgs).await().atMost(timeout);
    }

    @Override
    public boolean pexpire(K key, long ms) {
        return reactive.pexpire(key, ms).await().atMost(timeout);
    }

    @Override
    public boolean pexpire(K key, Duration duration) {
        return reactive.pexpire(key, duration).await().atMost(timeout);
    }

    @Override
    public boolean pexpireat(K key, long timestamp) {
        return reactive.pexpireat(key, timestamp).await().atMost(timeout);
    }

    @Override
    public boolean pexpireat(K key, Instant timestamp) {
        return reactive.pexpireat(key, timestamp).await().atMost(timeout);
    }

    @Override
    public boolean pexpireat(K key, long timestamp, ExpireArgs pexpireArgs) {
        return reactive.pexpireat(key, timestamp, pexpireArgs).await().atMost(timeout);
    }

    @Override
    public boolean pexpireat(K key, Instant timestamp, ExpireArgs pexpireArgs) {
        return reactive.pexpireat(key, timestamp, pexpireArgs).await().atMost(timeout);
    }

    @Override
    public long pexpiretime(K key) {
        return reactive.pexpiretime(key).await().atMost(timeout);
    }

    @Override
    public long pttl(K key) {
        return reactive.pttl(key).await().atMost(timeout);
    }

    @Override
    public K randomkey() {
        return reactive.randomkey().await().atMost(timeout);
    }

    @Override
    public void rename(K key, K newkey) {
        reactive.rename(key, newkey).await().atMost(timeout);
    }

    @Override
    public boolean renamenx(K key, K newkey) {
        return reactive.renamenx(key, newkey).await().atMost(timeout);
    }

    @Override
    public KeyScanCursor<K> scan() {
        return new ScanBlockingCursorImpl<>(reactive.scan(), timeout);
    }

    @Override
    public KeyScanCursor<K> scan(KeyScanArgs args) {
        return new ScanBlockingCursorImpl<>(reactive.scan(args), timeout);
    }

    @Override
    public int touch(K... keys) {
        return reactive.touch(keys).await().atMost(timeout);
    }

    @Override
    public long ttl(K key) {
        return reactive.ttl(key).await().atMost(timeout);
    }

    @Override
    public RedisValueType type(K key) {
        return reactive.type(key).await().atMost(timeout);
    }

    @Override
    public int unlink(K... keys) {
        return reactive.unlink(keys).await().atMost(timeout);
    }
}
