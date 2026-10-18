package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.Map;

import io.quarkus.redis.datasource.string.TransactionalStringCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.quarkus.redis.datasource.value.TransactionalValueCommands;

public class BlockingTransactionalStringCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalStringCommands<K, V>, TransactionalValueCommands<K, V> {

    private final ReactiveTransactionalValueCommands<K, V> reactive;

    public BlockingTransactionalStringCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalValueCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void append(K key, V value) {
        this.reactive.append(key, value).await().atMost(this.timeout);
    }

    @Override
    public void decr(K key) {
        this.reactive.decr(key).await().atMost(this.timeout);
    }

    @Override
    public void decrby(K key, long amount) {
        this.reactive.decrby(key, amount).await().atMost(this.timeout);
    }

    @Override
    public void get(K key) {
        this.reactive.get(key).await().atMost(this.timeout);
    }

    @Override
    public void getdel(K key) {
        this.reactive.getdel(key).await().atMost(this.timeout);
    }

    @Override
    public void getex(K key, io.quarkus.redis.datasource.string.GetExArgs args) {
        this.reactive.getex(key, args).await().atMost(this.timeout);
    }

    @Override
    public void getex(K key, io.quarkus.redis.datasource.value.GetExArgs args) {
        this.reactive.getex(key, args).await().atMost(this.timeout);
    }

    @Override
    public void getrange(K key, long start, long end) {
        this.reactive.getrange(key, start, end).await().atMost(this.timeout);
    }

    @Override
    public void getset(K key, V value) {
        this.reactive.getset(key, value).await().atMost(this.timeout);
    }

    @Override
    public void incr(K key) {
        this.reactive.incr(key).await().atMost(this.timeout);
    }

    @Override
    public void incrby(K key, long amount) {
        this.reactive.incrby(key, amount).await().atMost(this.timeout);
    }

    @Override
    public void incrbyfloat(K key, double amount) {
        this.reactive.incrbyfloat(key, amount).await().atMost(this.timeout);
    }

    @Override
    public void lcs(K key1, K key2) {
        this.reactive.lcs(key1, key2).await().atMost(this.timeout);
    }

    @Override
    public void lcsLength(K key1, K key2) {
        this.reactive.lcsLength(key1, key2).await().atMost(this.timeout);
    }

    @Override
    public void mget(K... keys) {
        this.reactive.mget(keys).await().atMost(this.timeout);
    }

    @Override
    public void mset(Map<K, V> map) {
        this.reactive.mset(map).await().atMost(this.timeout);
    }

    @Override
    public void msetnx(Map<K, V> map) {
        this.reactive.msetnx(map).await().atMost(this.timeout);
    }

    @Override
    public void psetex(K key, long milliseconds, V value) {
        this.reactive.psetex(key, milliseconds, value).await().atMost(this.timeout);
    }

    @Override
    public void set(K key, V value) {
        this.reactive.set(key, value).await().atMost(this.timeout);
    }

    @Override
    public void set(K key, V value, io.quarkus.redis.datasource.string.SetArgs setArgs) {
        this.reactive.set(key, value, setArgs).await().atMost(this.timeout);
    }

    @Override
    public void set(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        this.reactive.set(key, value, setArgs).await().atMost(this.timeout);
    }

    @Override
    public void setAndChanged(K key, V value) {
        this.reactive.setAndChanged(key, value).await().atMost(this.timeout);
    }

    @Override
    public void setAndChanged(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        this.reactive.setAndChanged(key, value, setArgs).await().atMost(this.timeout);
    }

    @Override
    public void setGet(K key, V value) {
        this.reactive.setGet(key, value).await().atMost(this.timeout);
    }

    @Override
    public void setGet(K key, V value, io.quarkus.redis.datasource.string.SetArgs setArgs) {
        this.reactive.setGet(key, value, setArgs).await().atMost(this.timeout);
    }

    @Override
    public void setGet(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        this.reactive.setGet(key, value, setArgs).await().atMost(this.timeout);
    }

    @Override
    public void setex(K key, long seconds, V value) {
        this.reactive.setex(key, seconds, value).await().atMost(this.timeout);
    }

    @Override
    public void setnx(K key, V value) {
        this.reactive.setnx(key, value).await().atMost(this.timeout);
    }

    @Override
    public void setrange(K key, long offset, V value) {
        this.reactive.setrange(key, offset, value).await().atMost(this.timeout);
    }

    @Override
    public void strlen(K key) {
        this.reactive.strlen(key).await().atMost(this.timeout);
    }
}
