package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.SortArgs;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.quarkus.redis.datasource.set.SScanCursor;
import io.quarkus.redis.datasource.set.SetCommands;

public class BlockingSetCommandsImpl<K, V> extends AbstractRedisCommandGroup implements SetCommands<K, V> {

    private final ReactiveSetCommands<K, V> reactive;

    public BlockingSetCommandsImpl(RedisDataSource ds, ReactiveSetCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public int sadd(K key, V... values) {
        return reactive.sadd(key, values).await().atMost(timeout);
    }

    @Override
    public long scard(K key) {
        return reactive.scard(key).await().atMost(timeout);
    }

    @Override
    public Set<V> sdiff(K... keys) {
        return reactive.sdiff(keys).await().atMost(timeout);
    }

    @Override
    public long sdiffstore(K destination, K... keys) {
        return reactive.sdiffstore(destination, keys).await().atMost(timeout);
    }

    @Override
    public Set<V> sinter(K... keys) {
        return reactive.sinter(keys).await().atMost(timeout);
    }

    @Override
    public long sintercard(K... keys) {
        return reactive.sintercard(keys).await().atMost(timeout);
    }

    @Override
    public long sintercard(int limit, K... keys) {
        return reactive.sintercard(limit, keys).await().atMost(timeout);
    }

    @Override
    public long sinterstore(K destination, K... keys) {
        return reactive.sinterstore(destination, keys).await().atMost(timeout);
    }

    @Override
    public boolean sismember(K key, V member) {
        return reactive.sismember(key, member).await().atMost(timeout);
    }

    @Override
    public Set<V> smembers(K key) {
        return reactive.smembers(key).await().atMost(timeout);
    }

    @Override
    public List<Boolean> smismember(K key, V... members) {
        return reactive.smismember(key, members).await().atMost(timeout);
    }

    @Override
    public boolean smove(K source, K destination, V member) {
        return reactive.smove(source, destination, member).await().atMost(timeout);
    }

    @Override
    public V spop(K key) {
        return reactive.spop(key).await().atMost(timeout);
    }

    @Override
    public Set<V> spop(K key, int count) {
        return reactive.spop(key, count).await().atMost(timeout);
    }

    @Override
    public V srandmember(K key) {
        return reactive.srandmember(key).await().atMost(timeout);
    }

    @Override
    public List<V> srandmember(K key, int count) {
        return reactive.srandmember(key, count).await().atMost(timeout);
    }

    @Override
    public int srem(K key, V... members) {
        return reactive.srem(key, members).await().atMost(timeout);
    }

    @Override
    public Set<V> sunion(K... keys) {
        return reactive.sunion(keys).await().atMost(timeout);
    }

    @Override
    public long sunionstore(K destination, K... keys) {
        return reactive.sunionstore(destination, keys).await().atMost(timeout);
    }

    @Override
    public SScanCursor<V> sscan(K key) {
        return new SScanBlockingCursorImpl<>(reactive.sscan(key), timeout);
    }

    @Override
    public SScanCursor<V> sscan(K key, ScanArgs scanArgs) {
        return new SScanBlockingCursorImpl<>(reactive.sscan(key, scanArgs), timeout);
    }

    @Override
    public List<V> sort(K key) {
        return reactive.sort(key).await().atMost(timeout);
    }

    @Override
    public List<V> sort(K key, SortArgs sortArguments) {
        return reactive.sort(key, sortArguments).await().atMost(timeout);
    }

    @Override
    public long sortAndStore(K key, K destination, SortArgs sortArguments) {
        return reactive.sortAndStore(key, destination, sortArguments).await().atMost(timeout);
    }

    @Override
    public long sortAndStore(K key, K destination) {
        return reactive.sortAndStore(key, destination).await().atMost(timeout);
    }
}
