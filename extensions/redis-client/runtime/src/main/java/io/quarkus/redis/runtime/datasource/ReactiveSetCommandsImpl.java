package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.ScanArgs;
import io.quarkus.redis.datasource.set.ReactiveSScanCursor;
import io.quarkus.redis.datasource.set.ReactiveSetCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveSetCommandsImpl<K, V> extends AbstractSetCommands<K, V> implements ReactiveSetCommands<K, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveSetCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Integer> sadd(K key, V... members) {
        return super._sadd(key, members).map(Response::toInteger);
    }

    @Override
    public Uni<Long> scard(K key) {
        return super._scard(key).map(Response::toLong);
    }

    @Override
    public Uni<Set<V>> sdiff(K... keys) {
        return super._sdiff(keys).map(this::decodeAsSetOfValues);
    }

    @Override
    public Uni<Long> sdiffstore(K destination, K... keys) {
        return super._sdiffstore(destination, keys).map(Response::toLong);
    }

    @Override
    public Uni<Set<V>> sinter(K... keys) {
        return super._sinter(keys).map(this::decodeAsSetOfValues);
    }

    @Override
    public Uni<Long> sintercard(K... keys) {
        return super._sintercard(keys).map(Response::toLong);
    }

    @Override
    public Uni<Long> sintercard(int limit, K... keys) {
        return super._sintercard(limit, keys).map(Response::toLong);
    }

    @Override
    public Uni<Long> sinterstore(K destination, K... keys) {
        return super._sinterstore(destination, keys).map(Response::toLong);
    }

    @Override
    public Uni<Boolean> sismember(K key, V member) {
        return super._sismember(key, member).map(Response::toBoolean);
    }

    @Override
    public Uni<Set<V>> smembers(K key) {
        return super._smembers(key).map(this::decodeAsSetOfValues);
    }

    @Override
    public Uni<List<Boolean>> smismember(K key, V... members) {
        return super._smismember(key, members).map(this::decodeAsListOfBooleans);
    }

    @Override
    public Uni<Boolean> smove(K source, K destination, V member) {
        nonNull(source, "source");
        nonNull(destination, "destination");
        nonNull(member, "member");
        return super._smove(source, destination, member).map(Response::toBoolean);
    }

    @Override
    public Uni<V> spop(K key) {
        return super._spop(key).map(this::decodeV);
    }

    @Override
    public Uni<Set<V>> spop(K key, int count) {
        return super._spop(key, count).map(this::decodeAsSetOfValues);
    }

    @Override
    public Uni<V> srandmember(K key) {
        return super._srandmember(key).map(this::decodeV);
    }

    @Override
    public Uni<List<V>> srandmember(K key, int count) {
        return super._srandmember(key, count).map(this::decodeListOfValues);
    }

    @Override
    public Uni<Integer> srem(K key, V... members) {
        return super._srem(key, members).map(Response::toInteger);
    }

    @Override
    public Uni<Set<V>> sunion(K... keys) {
        return super._sunion(keys).map(this::decodeAsSetOfValues);
    }

    @Override
    public Uni<Long> sunionstore(K destination, K... keys) {
        return super._sunionstore(destination, keys).map(Response::toLong);
    }

    @Override
    public ReactiveSScanCursor<V> sscan(K key) {
        return new SScanReactiveCursorImpl<>(redis, key, marshaller, typeOfValue, Collections.emptyList());
    }

    @Override
    public ReactiveSScanCursor<V> sscan(K key, ScanArgs scanArgs) {
        nonNull(scanArgs, "scanArgs");
        return new SScanReactiveCursorImpl<>(redis, key, marshaller, typeOfValue, scanArgs.toArgs());
    }

}
