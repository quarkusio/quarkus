package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.set.ReactiveTransactionalSetCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalSetCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalSetCommands<K, V> {

    private final ReactiveSetCommandsImpl<K, V> reactive;

    public ReactiveTransactionalSetCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveSetCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> sadd(K key, V... values) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._sadd(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> scard(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._scard(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sdiff(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsSetOfValues);
        return this.reactive._sdiff(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sdiffstore(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._sdiffstore(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sinter(K... keys) {
        this.tx.enqueue(this.reactive::decodeAsSetOfValues);
        return this.reactive._sinter(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sintercard(K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._sintercard(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sintercard(int limit, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._sintercard(limit, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sinterstore(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._sinterstore(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sismember(K key, V member) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._sismember(key, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> smembers(K key) {
        this.tx.enqueue(this.reactive::decodeAsSetOfValues);
        return this.reactive._smembers(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> smismember(K key, V... members) {
        this.tx.enqueue(this.reactive::decodeAsListOfBooleans);
        return this.reactive._smismember(key, members).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> smove(K source, K destination, V member) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._smove(source, destination, member).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> spop(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._spop(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> spop(K key, int count) {
        this.tx.enqueue(this.reactive::decodeAsSetOfValues);
        return this.reactive._spop(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> srandmember(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._srandmember(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> srandmember(K key, int count) {
        this.tx.enqueue(this.reactive::decodeListOfValues);
        return this.reactive._srandmember(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> srem(K key, V... members) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._srem(key, members).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sunion(K... keys) {
        this.tx.enqueue(resp -> this.reactive.decodeAsSetOfValues(resp));
        return this.reactive._sunion(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> sunionstore(K destination, K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._sunionstore(destination, keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
