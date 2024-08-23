package io.quarkus.redis.runtime.datasource;

import java.util.Map;

import io.quarkus.redis.datasource.string.GetExArgs;
import io.quarkus.redis.datasource.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.string.SetArgs;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalStringCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalStringCommands<K, V>, ReactiveTransactionalValueCommands<K, V> {

    private final ReactiveStringCommandsImpl<K, V> reactive;

    public ReactiveTransactionalStringCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveStringCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> append(K key, V value) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._append(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> decr(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._decr(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> decrby(K key, long amount) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._decrby(key, amount).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> get(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._get(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> getdel(K key) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._getdel(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> getex(K key, GetExArgs args) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._getex(key, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> getex(K key, io.quarkus.redis.datasource.value.GetExArgs args) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._getex(key, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> getrange(K key, long start, long end) {
        this.tx.enqueue(Response::toString);
        return this.reactive._getrange(key, start, end).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> getset(K key, V value) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._getset(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> incr(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._incr(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> incrby(K key, long amount) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._incrby(key, amount).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> incrbyfloat(K key, double amount) {
        this.tx.enqueue(Response::toDouble);
        return this.reactive._incrbyfloat(key, amount).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lcs(K key1, K key2) {
        this.tx.enqueue(Response::toString);
        return this.reactive._lcs(key1, key2).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> lcsLength(K key1, K key2) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._lcsLength(key1, key2).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> mget(K... keys) {
        this.tx.enqueue(resp -> this.reactive.decodeAsOrderedMap(resp, keys));
        return this.reactive._mget(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> mset(Map<K, V> map) {
        this.tx.enqueue(resp -> null);
        return this.reactive._mset(map).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> msetnx(Map<K, V> map) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._msetnx(map).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> psetex(K key, long milliseconds, V value) {
        this.tx.enqueue(resp -> null);
        return this.reactive._psetex(key, milliseconds, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> set(K key, V value) {
        this.tx.enqueue(resp -> null);
        return this.reactive._set(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> set(K key, V value, SetArgs setArgs) {
        this.tx.enqueue(resp -> null);
        return this.reactive._set(key, value, setArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> set(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        this.tx.enqueue(resp -> null);
        return this.reactive._set(key, value, setArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setGet(K key, V value) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._setGet(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setGet(K key, V value, SetArgs setArgs) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._setGet(key, value, setArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setGet(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        this.tx.enqueue(this.reactive::decodeV);
        return this.reactive._setGet(key, value, setArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setex(K key, long seconds, V value) {
        this.tx.enqueue(resp -> null);
        return this.reactive._setex(key, seconds, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setnx(K key, V value) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._setnx(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> setrange(K key, long offset, V value) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._setrange(key, offset, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> strlen(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._strlen(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
