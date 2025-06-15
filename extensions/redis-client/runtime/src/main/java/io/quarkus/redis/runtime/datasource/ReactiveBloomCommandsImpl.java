package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.ReactiveBloomCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveBloomCommandsImpl<K, V> extends AbstractBloomCommands<K, V>
        implements ReactiveBloomCommands<K, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveBloomCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Boolean> bfadd(K key, V value) {
        return _bfadd(key, value).map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> bfexists(K key, V value) {
        return _bfexists(key, value).map(Response::toBoolean);
    }

    @Override
    public Uni<List<Boolean>> bfmadd(K key, V... values) {
        return _bfmadd(key, values).map(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
    }

    static List<Boolean> decodeAsListOfBooleans(Response r) {
        List<Boolean> results = new ArrayList<>();
        if (r == null) {
            return results;
        }
        for (Response nested : r) {
            results.add(nested.toBoolean());
        }
        return results;
    }

    @Override
    public Uni<List<Boolean>> bfmexists(K key, V... values) {
        return _bfmexists(key, values).map(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity) {
        return bfreserve(key, errorRate, capacity, new BfReserveArgs());
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        return _bfreserve(key, errorRate, capacity, args).replaceWithVoid();
    }

    @Override
    public Uni<List<Boolean>> bfinsert(K key, BfInsertArgs args, V... values) {
        return _bfinsert(key, args, values).map(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
    }
}
