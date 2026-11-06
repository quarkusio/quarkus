package io.quarkus.redis.lettuce.runtime.internal.key;

import java.time.Duration;
import java.time.Instant;

import io.quarkus.redis.datasource.keys.CopyArgs;
import io.quarkus.redis.datasource.keys.ExpireArgs;
import io.quarkus.redis.datasource.keys.ReactiveTransactionalKeyCommands;
import io.quarkus.redis.datasource.keys.RedisKeyNotFoundException;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalKeyCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveKeyCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._xxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * as the Vert.x backend.
 *
 * @param <K> the key type
 */
public class LettuceReactiveTransactionalKeyCommandsImpl<K>
        implements ReactiveTransactionalKeyCommands<K> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveKeyCommandsImpl<K> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalKeyCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveKeyCommandsImpl<K> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> copy(K source, K destination) {
        return tx.enqueue(reactive._copy(source, destination));
    }

    @Override
    public Uni<Void> copy(K source, K destination, CopyArgs copyArgs) {
        return tx.enqueue(reactive._copy(source, destination, copyArgs));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> del(K... keys) {
        return tx.enqueue(reactive._del(keys));
    }

    @Override
    public Uni<Void> dump(K key) {
        return tx.enqueue(reactive._dump(key));
    }

    @Override
    public Uni<Void> exists(K key) {
        return tx.enqueue(reactive._exists(key));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> exists(K... keys) {
        return tx.enqueue(reactive._exists(keys));
    }

    @Override
    public Uni<Void> expire(K key, long seconds, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._expire(key, seconds, expireArgs));
    }

    @Override
    public Uni<Void> expire(K key, Duration duration, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._expire(key, duration, expireArgs));
    }

    @Override
    public Uni<Void> expire(K key, long seconds) {
        return tx.enqueue(reactive._expire(key, seconds));
    }

    @Override
    public Uni<Void> expire(K key, Duration duration) {
        return tx.enqueue(reactive._expire(key, duration));
    }

    @Override
    public Uni<Void> expireat(K key, long timestamp) {
        return tx.enqueue(reactive._expireat(key, timestamp));
    }

    @Override
    public Uni<Void> expireat(K key, Instant timestamp) {
        return tx.enqueue(reactive._expireat(key, timestamp));
    }

    @Override
    public Uni<Void> expireat(K key, long timestamp, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._expireat(key, timestamp, expireArgs));
    }

    @Override
    public Uni<Void> expireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._expireat(key, timestamp, expireArgs));
    }

    @Override
    public Uni<Void> expiretime(K key) {
        return tx.enqueue(reactive._expiretime(key));
    }

    @Override
    public Uni<Void> keys(String pattern) {
        return tx.enqueue(reactive._keys(pattern));
    }

    @Override
    public Uni<Void> move(K key, long db) {
        return tx.enqueue(reactive._move(key, db));
    }

    @Override
    public Uni<Void> persist(K key) {
        return tx.enqueue(reactive._persist(key));
    }

    @Override
    public Uni<Void> pexpire(K key, long milliseconds, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._pexpire(key, milliseconds, expireArgs));
    }

    @Override
    public Uni<Void> pexpire(K key, Duration duration, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._pexpire(key, duration, expireArgs));
    }

    @Override
    public Uni<Void> pexpire(K key, long ms) {
        return tx.enqueue(reactive._pexpire(key, ms));
    }

    @Override
    public Uni<Void> pexpire(K key, Duration duration) {
        return tx.enqueue(reactive._pexpire(key, duration));
    }

    @Override
    public Uni<Void> pexpireat(K key, long timestamp) {
        return tx.enqueue(reactive._pexpireat(key, timestamp));
    }

    @Override
    public Uni<Void> pexpireat(K key, Instant timestamp) {
        return tx.enqueue(reactive._pexpireat(key, timestamp));
    }

    @Override
    public Uni<Void> pexpireat(K key, long timestamp, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._pexpireat(key, timestamp, expireArgs));
    }

    @Override
    public Uni<Void> pexpireat(K key, Instant timestamp, ExpireArgs expireArgs) {
        return tx.enqueue(reactive._pexpireat(key, timestamp, expireArgs));
    }

    @Override
    public Uni<Void> pexpiretime(K key) {
        return tx.enqueue(reactive._pexpiretime(key));
    }

    @Override
    public Uni<Void> pttl(K key) {
        return tx.enqueue(reactive._pttl(key));
    }

    @Override
    public Uni<Void> randomkey() {
        return tx.enqueue(reactive._randomkey());
    }

    @Override
    public Uni<Void> rename(K key, K newkey) {
        return tx.enqueue(reactive._rename(key, newkey));
    }

    @Override
    public Uni<Void> renamenx(K key, K newkey) {
        return tx.enqueue(reactive._renamenx(key, newkey));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> touch(K... keys) {
        return tx.enqueue(reactive._touch(keys));
    }

    @Override
    public Uni<Void> ttl(K key) throws RedisKeyNotFoundException {
        return tx.enqueue(reactive._ttl(key));
    }

    @Override
    public Uni<Void> type(K key) {
        return tx.enqueue(reactive._type(key));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> unlink(K... keys) {
        return tx.enqueue(reactive._unlink(keys));
    }

}
