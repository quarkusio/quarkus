package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.ReactiveBitMapCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveBitMapCommandsImpl<K> extends AbstractBitMapCommands<K>
        implements ReactiveBitMapCommands<K>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;

    public ReactiveBitMapCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k) {
        super(redis, k);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Long> bitcount(K key) {
        return super._bitcount(key).map(Response::toLong);
    }

    @Override
    public Uni<Long> bitcount(K key, long start, long end) {
        return super._bitcount(key, start, end).map(Response::toLong);
    }

    @Override
    public Uni<List<Long>> bitfield(K key, BitFieldArgs bitFieldArgs) {
        return super._bitfield(key, bitFieldArgs).map(this::decodeListOfLongs);
    }

    @Override
    public Uni<Long> bitpos(K key, int bit) {
        return super._bitpos(key, bit).map(Response::toLong);
    }

    @Override
    public Uni<Long> bitpos(K key, int bit, long start) {
        return super._bitpos(key, bit, start).map(Response::toLong);
    }

    @Override
    public Uni<Long> bitpos(K key, int bit, long start, long end) {
        return super._bitpos(key, bit, start, end).map(Response::toLong);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> bitopAnd(K destination, K... keys) {
        return super._bitopAnd(destination, keys).map(Response::toLong);
    }

    @Override
    public Uni<Long> bitopNot(K destination, K source) {
        return super._bitopNot(destination, source).map(Response::toLong);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> bitopOr(K destination, K... keys) {
        return super._bitopOr(destination, keys).map(Response::toLong);
    }

    @SafeVarargs
    @Override
    public final Uni<Long> bitopXor(K destination, K... keys) {
        return super._bitopXor(destination, keys).map(Response::toLong);
    }

    @Override
    public Uni<Integer> setbit(K key, long offset, int value) {
        return super._setbit(key, offset, value).map(Response::toInteger);
    }

    @Override
    public Uni<Integer> getbit(K key, long offset) {
        return super._getbit(key, offset).map(Response::toInteger);
    }

}
