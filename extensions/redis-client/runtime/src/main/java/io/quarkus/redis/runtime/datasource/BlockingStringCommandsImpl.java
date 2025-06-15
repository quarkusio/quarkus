package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.string.GetExArgs;
import io.quarkus.redis.datasource.string.SetArgs;
import io.quarkus.redis.datasource.string.StringCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.quarkus.redis.datasource.value.ValueCommands;

public class BlockingStringCommandsImpl<K, V> extends AbstractRedisCommandGroup
        implements StringCommands<K, V>, ValueCommands<K, V> {

    private final ReactiveValueCommands<K, V> reactive;

    public BlockingStringCommandsImpl(RedisDataSource ds, ReactiveValueCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public long append(K key, V value) {
        return reactive.append(key, value).await().atMost(timeout);
    }

    @Override
    public long decr(K key) {
        return reactive.decr(key).await().atMost(timeout);
    }

    @Override
    public long decrby(K key, long amount) {
        return reactive.decrby(key, amount).await().atMost(timeout);
    }

    @Override
    public V get(K key) {
        return reactive.get(key).await().atMost(timeout);
    }

    @Override
    public V getdel(K key) {
        return reactive.getdel(key).await().atMost(timeout);
    }

    @Override
    public V getex(K key, GetExArgs args) {
        return reactive.getex(key, args).await().atMost(timeout);
    }

    @Override
    public V getex(K key, io.quarkus.redis.datasource.value.GetExArgs args) {
        return reactive.getex(key, args).await().atMost(timeout);
    }

    @Override
    public String getrange(K key, long start, long end) {
        return reactive.getrange(key, start, end).await().atMost(timeout);
    }

    @Override
    public V getset(K key, V value) {
        return reactive.getset(key, value).await().atMost(timeout);
    }

    @Override
    public long incr(K key) {
        return reactive.incr(key).await().atMost(timeout);
    }

    @Override
    public long incrby(K key, long amount) {
        return reactive.incrby(key, amount).await().atMost(timeout);
    }

    @Override
    public double incrbyfloat(K key, double amount) {
        return reactive.incrbyfloat(key, amount).await().atMost(timeout);
    }

    @Override
    public String lcs(K key1, K key2) {
        return reactive.lcs(key1, key2).await().atMost(timeout);
    }

    @Override
    public long lcsLength(K key1, K key2) {
        return reactive.lcsLength(key1, key2).await().atMost(timeout);
    }

    @Override
    public Map<K, V> mget(K... keys) {
        return reactive.mget(keys).await().atMost(timeout);
    }

    @Override
    public void mset(Map<K, V> map) {
        reactive.mset(map).await().atMost(timeout);
    }

    @Override
    public boolean msetnx(Map<K, V> map) {
        return reactive.msetnx(map).await().atMost(timeout);
    }

    @Override
    public void set(K key, V value) {
        reactive.set(key, value).await().atMost(timeout);
    }

    @Override
    public void set(K key, V value, SetArgs setArgs) {
        reactive.set(key, value, setArgs).await().atMost(timeout);
    }

    @Override
    public void set(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        reactive.set(key, value, setArgs).await().atMost(timeout);
    }

    @Override
    public V setGet(K key, V value) {
        return reactive.setGet(key, value).await().atMost(timeout);
    }

    @Override
    public V setGet(K key, V value, SetArgs setArgs) {
        return reactive.setGet(key, value, setArgs).await().atMost(timeout);
    }

    @Override
    public V setGet(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        return reactive.setGet(key, value, setArgs).await().atMost(timeout);
    }

    @Override
    public void setex(K key, long seconds, V value) {
        reactive.setex(key, seconds, value).await().atMost(timeout);
    }

    @Override
    public void psetex(K key, long milliseconds, V value) {
        reactive.psetex(key, milliseconds, value).await().atMost(timeout);
    }

    @Override
    public boolean setnx(K key, V value) {
        return reactive.setnx(key, value).await().atMost(timeout);
    }

    @Override
    public long setrange(K key, long offset, V value) {
        return reactive.setrange(key, offset, value).await().atMost(timeout);
    }

    @Override
    public long strlen(K key) {
        return reactive.strlen(key).await().atMost(timeout);
    }
}
