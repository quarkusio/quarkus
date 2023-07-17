package io.quarkus.redis.runtime.datasource;

import java.util.Map;

import io.quarkus.redis.datasource.hash.ReactiveTransactionalHashCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalHashCommandsImpl<K, F, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalHashCommands<K, F, V> {

    private final ReactiveHashCommandsImpl<K, F, V> reactive;

    public ReactiveTransactionalHashCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveHashCommandsImpl<K, F, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> hdel(K key, F... fields) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._hdel(key, fields).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hexists(K key, F field) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._hexists(key, field).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hget(K key, F field) {
        this.tx.enqueue(resp -> this.reactive.decodeV(resp));
        return this.reactive._hget(key, field).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hincrby(K key, F field, long amount) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._hincrby(key, field, amount).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hincrbyfloat(K key, F field, double amount) {
        this.tx.enqueue(Response::toDouble);
        return this.reactive._hincrbyfloat(key, field, amount).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hgetall(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeMap(resp));
        return this.reactive._hgetall(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hkeys(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeListOfField(resp));
        return this.reactive._hkeys(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hlen(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._hlen(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hmget(K key, F... fields) {
        this.tx.enqueue(resp -> this.reactive.decodeMap(resp));
        return this.reactive._hmget(key, fields).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Deprecated
    @Override
    public Uni<Void> hmset(K key, Map<F, V> map) {
        this.tx.enqueue(resp -> null);
        return this.reactive._hmset(key, map).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hrandfield(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeF(resp));
        return this.reactive._hrandfield(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hrandfield(K key, long count) {
        this.tx.enqueue(resp -> this.reactive.decodeListOfField(resp));
        return this.reactive._hrandfield(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hrandfieldWithValues(K key, long count) {
        this.tx.enqueue(resp -> this.reactive.decodeMap(resp));
        return this.reactive._hrandfieldWithValues(key, count).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hset(K key, F field, V value) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._hset(key, field, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hset(K key, Map<F, V> map) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._hset(key, map).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hsetnx(K key, F field, V value) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._hsetnx(key, field, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hstrlen(K key, F field) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._hstrlen(key, field).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> hvals(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeListOfField(resp));
        return this.reactive._hvals(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
