package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.bitmap.BitFieldArgs;
import io.quarkus.redis.datasource.bitmap.ReactiveTransactionalBitMapCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalBitMapCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalBitMapCommands<K> {

    private final ReactiveBitMapCommandsImpl<K> reactive;

    public ReactiveTransactionalBitMapCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveBitMapCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> bitcount(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitcount(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitcount(K key, long start, long end) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitcount(key, start, end).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> getbit(K key, long offset) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._getbit(key, offset).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitfield(K key, BitFieldArgs bitFieldArgs) {
        this.tx.enqueue(this.reactive::decodeListOfLongs);
        return this.reactive._bitfield(key, bitFieldArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitpos(K key, int valueToLookFor) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitpos(key, valueToLookFor).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitpos(K key, int bit, long start) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitpos(key, bit, start).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitpos(K key, int bit, long start, long end) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitpos(key, bit, start, end).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitopAnd(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitopAnd(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitopNot(K destination, K source) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitopNot(destination, source).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitopOr(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitopOr(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bitopXor(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._bitopXor(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setbit(K key, long offset, int value) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._setbit(key, offset, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
