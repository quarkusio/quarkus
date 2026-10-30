package io.quarkus.redis.lettuce.runtime.internal.bitmap;

import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTransactionalBitMapCommandsImpl<K> implements ReactiveTransactionalBitMapCommands<K> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveBitMapCommandsImpl<K> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalBitMapCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveBitMapCommandsImpl<K> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> bitcount(K key) {
        return tx.enqueue(reactive._bitcount(key));
    }

    @Override
    public Uni<Void> bitcount(K key, long start, long end) {
        return tx.enqueue(reactive._bitcount(key, start, end));
    }

    @Override
    public Uni<Void> getbit(K key, long offset) {
        return tx.enqueue(reactive._getbit(key, offset));
    }

    @Override
    public Uni<Void> bitfield(K key, BitFieldArgs bitFieldArgs) {
        return tx.enqueue(reactive._bitfield(key, bitFieldArgs));
    }

    @Override
    public Uni<Void> bitpos(K key, int valueToLookFor) {
        return tx.enqueue(reactive._bitpos(key, valueToLookFor));
    }

    @Override
    public Uni<Void> bitpos(K key, int bit, long start) {
        return tx.enqueue(reactive._bitpos(key, bit, start));
    }

    @Override
    public Uni<Void> bitpos(K key, int bit, long start, long end) {
        return tx.enqueue(reactive._bitpos(key, bit, start, end));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bitopAnd(K destination, K... keys) {
        return tx.enqueue(reactive._bitopAnd(destination, keys));
    }

    @Override
    public Uni<Void> bitopNot(K destination, K source) {
        return tx.enqueue(reactive._bitopNot(destination, source));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bitopOr(K destination, K... keys) {
        return tx.enqueue(reactive._bitopOr(destination, keys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bitopXor(K destination, K... keys) {
        return tx.enqueue(reactive._bitopXor(destination, keys));
    }

    @Override
    public Uni<Void> setbit(K key, long offset, int value) {
        return tx.enqueue(reactive._setbit(key, offset, value));
    }

}
