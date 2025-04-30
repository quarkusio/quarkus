package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.ReactiveCuckooCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveCuckooCommandsImpl<K, V> extends AbstractCuckooCommands<K, V>
        implements ReactiveCuckooCommands<K, V>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;

    public ReactiveCuckooCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Void> cfadd(K key, V value) {
        return super._cfadd(key, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Boolean> cfaddnx(K key, V value) {
        return super._cfaddnx(key, value)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Long> cfcount(K key, V value) {
        return super._cfcount(key, value)
                .map(Response::toLong);
    }

    @Override
    public Uni<Boolean> cfdel(K key, V value) {
        return super._cfdel(key, value)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Boolean> cfexists(K key, V value) {
        return super._cfexists(key, value)
                .map(Response::toBoolean);
    }

    @Override
    public Uni<Void> cfinsert(K key, V... values) {
        return super._cfinsert(key, values)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> cfinsert(K key, CfInsertArgs args, V... values) {
        return super._cfinsert(key, args, values)
                .replaceWithVoid();
    }

    @Override
    public Uni<List<Boolean>> cfinsertnx(K key, V... values) {
        return super._cfinsertnx(key, values)
                .map(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
    }

    @Override
    public Uni<List<Boolean>> cfinsertnx(K key, CfInsertArgs args, V... values) {
        return super._cfinsertnx(key, args, values)
                .map(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
    }

    @Override
    public Uni<List<Boolean>> cfmexists(K key, V... values) {
        return super._cfmexists(key, values)
                .map(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity) {
        return super._cfreserve(key, capacity)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity, CfReserveArgs args) {
        return super._cfreserve(key, capacity, args)
                .replaceWithVoid();
    }
}
