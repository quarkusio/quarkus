package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.string.GetExArgs;
import io.quarkus.redis.datasource.string.ReactiveStringCommands;
import io.quarkus.redis.datasource.string.SetArgs;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveStringCommandsImpl<K, V> extends AbstractStringCommands<K, V>
        implements ReactiveStringCommands<K, V>, ReactiveValueCommands<K, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveStringCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Void> set(K key, V value) {
        return super._set(key, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> set(K key, V value, SetArgs setArgs) {
        return super._set(key, value, setArgs)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> set(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        return super._set(key, value, setArgs)
                .replaceWithVoid();
    }

    @Override
    public Uni<V> setGet(K key, V value) {
        return super._setGet(key, value)
                .map(this::decodeV);
    }

    @Override
    public Uni<V> setGet(K key, V value, SetArgs setArgs) {
        return super._setGet(key, value, setArgs)
                .map(this::decodeV);
    }

    @Override
    public Uni<V> setGet(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        return super._setGet(key, value, setArgs)
                .map(this::decodeV);
    }

    @Override
    public Uni<Void> setex(K key, long seconds, V value) {
        return super._setex(key, seconds, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> psetex(K key, long milliseconds, V value) {
        return super._psetex(key, milliseconds, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Boolean> setnx(K key, V value) {
        return super._setnx(key, value)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Long> setrange(K key, long offset, V value) {
        return super._setrange(key, offset, value)
                .map(Response::toLong);
    }

    @Override
    public Uni<Long> strlen(K key) {
        return super._strlen(key)
                .map(Response::toLong);
    }

    @Override
    public Uni<Long> decr(K key) {
        return super._decr(key)
                .map(Response::toLong);
    }

    @Override
    public Uni<Long> decrby(K key, long amount) {
        return super._decrby(key, amount)
                .map(Response::toLong);
    }

    @Override
    public Uni<V> get(K key) {
        return super._get(key)
                .map(this::decodeV);
    }

    @Override
    public Uni<V> getdel(K key) {
        return super._getdel(key)
                .map(this::decodeV);
    }

    @Override
    public Uni<V> getex(K key, GetExArgs args) {
        return super._getex(key, args)
                .map(this::decodeV);
    }

    @Override
    public Uni<V> getex(K key, io.quarkus.redis.datasource.value.GetExArgs args) {
        return super._getex(key, args)
                .map(this::decodeV);
    }

    @Override
    public Uni<String> getrange(K key, long start, long end) {
        return super._getrange(key, start, end)
                .map(Response::toString);
    }

    @Override
    public Uni<V> getset(K key, V value) {
        return super._getset(key, value)
                .map(this::decodeV);
    }

    @Override
    public Uni<Long> incr(K key) {
        return super._incr(key)
                .map(Response::toLong);
    }

    @Override
    public Uni<Long> incrby(K key, long amount) {
        return super._incrby(key, amount)
                .map(Response::toLong);
    }

    @Override
    public Uni<Double> incrbyfloat(K key, double amount) {
        return super._incrbyfloat(key, amount)
                .map(Response::toDouble);
    }

    @Override
    public Uni<Long> append(K key, V value) {
        return super._append(key, value)
                .map(Response::toLong);
    }

    @Override
    public Uni<Map<K, V>> mget(K... keys) {
        return super._mget(keys)
                .map(r -> decodeAsOrderedMap(r, keys));
    }

    @Override
    public Uni<Void> mset(Map<K, V> map) {
        return super._mset(map)
                .replaceWithVoid();
    }

    @Override
    public Uni<Boolean> msetnx(Map<K, V> map) {
        return super._msetnx(map)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<String> lcs(K key1, K key2) {
        return super._lcs(key1, key2)
                .map(Response::toString);
    }

    @Override
    public Uni<Long> lcsLength(K key1, K key2) {
        return super._lcsLength(key1, key2)
                .map(Response::toLong);
    }
}
