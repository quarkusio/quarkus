package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.hash.ReactiveHashScanCursor;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveHashCommandsImpl<K, F, V> extends AbstractHashCommands<K, F, V> implements ReactiveHashCommands<K, F, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveHashCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type f, Type v) {
        super(redis, k, f, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @SafeVarargs
    @Override
    public final Uni<Integer> hdel(K key, F... fields) {
        return super._hdel(key, fields)
                .map(Response::toInteger);
    }

    @Override
    public Uni<Boolean> hexists(K key, F field) {
        return super._hexists(key, field)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<V> hget(K key, F field) {
        return super._hget(key, field)
                .map(this::decodeV);
    }

    @Override
    public Uni<Long> hincrby(K key, F field, long amount) {
        return super._hincrby(key, field, amount)
                .map(Response::toLong);
    }

    @Override
    public Uni<Double> hincrbyfloat(K key, F field, double amount) {
        return super._hincrbyfloat(key, field, amount)
                .map(Response::toDouble);
    }

    @Override
    public Uni<Map<F, V>> hgetall(K key) {
        return super._hgetall(key)
                .map(this::decodeMap);
    }

    @Override
    public Uni<List<F>> hkeys(K key) {
        return super._hkeys(key)
                .map(this::decodeListOfField);
    }

    @Override
    public Uni<Long> hlen(K key) {
        return super._hlen(key)
                .map(Response::toLong);
    }

    @SafeVarargs
    @Override
    public final Uni<Map<F, V>> hmget(K key, F... fields) {
        return super._hmget(key, fields)
                .map(r -> decodeOrderedMap(r, fields));
    }

    @Override
    public Uni<Void> hmset(K key, Map<F, V> map) {
        return super._hmset(key, map).replaceWithVoid();
    }

    @Override
    public Uni<F> hrandfield(K key) {
        return super._hrandfield(key)
                .map(this::decodeF);
    }

    @Override
    public Uni<List<F>> hrandfield(K key, long count) {
        return super._hrandfield(key, count)
                .map(this::decodeListOfField);
    }

    @Override
    public Uni<Map<F, V>> hrandfieldWithValues(K key, long count) {
        return super._hrandfieldWithValues(key, count)
                .map(this::decodeFieldWithValueMap);
    }

    @Override
    public Uni<Boolean> hset(K key, F field, V value) {
        return super._hset(key, field, value)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Long> hset(K key, Map<F, V> map) {
        return super._hset(key, map)
                .map(Response::toLong);
    }

    @Override
    public Uni<Boolean> hsetnx(K key, F field, V value) {
        return super._hsetnx(key, field, value)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Long> hstrlen(K key, F field) {
        return super._hstrlen(key, field)
                .map(Response::toLong);
    }

    @Override
    public Uni<List<V>> hvals(K key) {
        return super._hvals(key)
                .map(this::decodeListOfValue);
    }

    @Override
    public ReactiveHashScanCursor<F, V> hscan(K key) {
        nonNull(key, "key");
        return new HScanReactiveCursorImpl<>(redis, key,
                marshaller, typeOfField, typeOfValue, Collections.emptyList());
    }

    @Override
    public ReactiveHashScanCursor<F, V> hscan(K key, ScanArgs scanArgs) {
        nonNull(key, "key");
        nonNull(scanArgs, "scanArgs");
        return new HScanReactiveCursorImpl<>(redis, key,
                marshaller, typeOfField, typeOfValue, scanArgs.toArgs());
    }

}
