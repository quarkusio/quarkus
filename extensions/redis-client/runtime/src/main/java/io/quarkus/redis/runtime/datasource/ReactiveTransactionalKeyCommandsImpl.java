package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalKeyCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalKeyCommands<K> {

    private final ReactiveKeyCommandsImpl<K> reactive;

    public ReactiveTransactionalKeyCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveKeyCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> copy(K source, K destination) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._copy(source, destination).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> copy(K source, K destination, CopyArgs copyArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._copy(source, destination, copyArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> del(K... keys) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._del(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> dump(K key) {
        this.tx.enqueue(Response::toString);
        return this.reactive._dump(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> exists(K key) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._exists(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> exists(K... keys) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._exists(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expire(K key, long seconds, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expire(key, seconds, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expire(K key, Duration duration, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expire(key, duration, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expire(K key, long seconds) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expire(key, seconds).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expire(K key, Duration duration) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expire(key, duration).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expireat(K key, long timestamp) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expireat(key, timestamp).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expireat(K key, Instant timestamp) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expireat(key, timestamp).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expireat(K key, long timestamp, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expireat(key, timestamp, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._expireat(key, timestamp, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> expiretime(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeExpireResponse(key, resp));
        return this.reactive._expiretime(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> keys(String pattern) {
        this.tx.enqueue(this.reactive::decodeKeys);
        return this.reactive._keys(pattern).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> move(K key, long db) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._move(key, db).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> persist(K key) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._persist(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpire(key, duration, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpire(K key, long ms) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpire(key, ms).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpire(K key, Duration duration) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpire(key, duration).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpire(key, milliseconds, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpireat(K key, long timestamp) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpireat(key, timestamp).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpireat(K key, Instant timestamp) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpireat(key, timestamp).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpireat(key, timestamp, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pexpireat(key, timestamp, expireArgs).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pexpiretime(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeExpireResponse(key, resp));
        return this.reactive._pexpiretime(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pttl(K key) {
        this.tx.enqueue(resp -> this.reactive.decodeExpireResponse(key, resp));
        return this.reactive._pttl(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> randomkey() {
        this.tx.enqueue(this.reactive::decodeK);
        return this.reactive._randomkey().invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> rename(K key, K newkey) {
        this.tx.enqueue(resp -> null);
        return this.reactive._rename(key, newkey).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> renamenx(K key, K newkey) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._renamenx(key, newkey).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> touch(K... keys) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._touch(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ttl(K key) throws RedisKeyNotFoundException {
        this.tx.enqueue(resp -> this.reactive.decodeExpireResponse(key, resp));
        return this.reactive._ttl(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> type(K key) {
        this.tx.enqueue(this.reactive::decodeRedisType);
        return this.reactive._type(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> unlink(K... keys) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._unlink(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
