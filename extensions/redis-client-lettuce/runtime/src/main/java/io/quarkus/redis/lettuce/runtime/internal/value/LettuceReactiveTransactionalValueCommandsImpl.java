package io.quarkus.redis.lettuce.runtime.internal.value;

import java.util.Map;

import io.quarkus.redis.datasource.string.ReactiveTransactionalStringCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.datasource.value.GetExArgs;
import io.quarkus.redis.datasource.value.ReactiveTransactionalValueCommands;
import io.quarkus.redis.datasource.value.SetArgs;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalValueCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveValueCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._xxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * as the Vert.x backend.
 * <p>
 * Also implements the deprecated {@link ReactiveTransactionalStringCommands} so that
 * {@code tx.string(...)} and {@code tx.value(...)} share one implementation, as on the Vert.x
 * backend. The {@code string}-package argument types extend their {@code value}-package
 * counterparts, so those overloads simply delegate.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LettuceReactiveTransactionalValueCommandsImpl<K, V>
        implements ReactiveTransactionalValueCommands<K, V>, ReactiveTransactionalStringCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveValueCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalValueCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveValueCommandsImpl<K, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> append(K key, V value) {
        return tx.enqueue(reactive._append(key, value));
    }

    @Override
    public Uni<Void> decr(K key) {
        return tx.enqueue(reactive._decr(key));
    }

    @Override
    public Uni<Void> decrby(K key, long amount) {
        return tx.enqueue(reactive._decrby(key, amount));
    }

    @Override
    public Uni<Void> get(K key) {
        return tx.enqueue(reactive._get(key));
    }

    @Override
    public Uni<Void> getdel(K key) {
        return tx.enqueue(reactive._getdel(key));
    }

    @Override
    public Uni<Void> getex(K key, GetExArgs args) {
        return tx.enqueue(reactive._getex(key, args));
    }

    @Override
    public Uni<Void> getex(K key, io.quarkus.redis.datasource.string.GetExArgs args) {
        return getex(key, (GetExArgs) args);
    }

    @Override
    public Uni<Void> getrange(K key, long start, long end) {
        return tx.enqueue(reactive._getrange(key, start, end));
    }

    @Override
    public Uni<Void> getset(K key, V value) {
        return tx.enqueue(reactive._getset(key, value));
    }

    @Override
    public Uni<Void> incr(K key) {
        return tx.enqueue(reactive._incr(key));
    }

    @Override
    public Uni<Void> incrby(K key, long amount) {
        return tx.enqueue(reactive._incrby(key, amount));
    }

    @Override
    public Uni<Void> incrbyfloat(K key, double amount) {
        return tx.enqueue(reactive._incrbyfloat(key, amount));
    }

    @Override
    public Uni<Void> lcs(K key1, K key2) {
        return tx.enqueue(reactive._lcs(key1, key2));
    }

    @Override
    public Uni<Void> lcsLength(K key1, K key2) {
        return tx.enqueue(reactive._lcsLength(key1, key2));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> mget(K... keys) {
        return tx.enqueue(reactive._mget(keys));
    }

    @Override
    public Uni<Void> mset(Map<K, V> map) {
        return tx.enqueue(reactive._mset(map));
    }

    @Override
    public Uni<Void> msetnx(Map<K, V> map) {
        return tx.enqueue(reactive._msetnx(map));
    }

    @Override
    public Uni<Void> psetex(K key, long milliseconds, V value) {
        return tx.enqueue(reactive._psetex(key, milliseconds, value));
    }

    @Override
    public Uni<Void> set(K key, V value) {
        return tx.enqueue(reactive._set(key, value).discarding());
    }

    @Override
    public Uni<Void> set(K key, V value, SetArgs setArgs) {
        return tx.enqueue(reactive._set(key, value, setArgs).discarding());
    }

    @Override
    public Uni<Void> set(K key, V value, io.quarkus.redis.datasource.string.SetArgs setArgs) {
        return set(key, value, (SetArgs) setArgs);
    }

    @Override
    public Uni<Void> setAndChanged(K key, V value) {
        return tx.enqueue(reactive._set(key, value));
    }

    @Override
    public Uni<Void> setAndChanged(K key, V value, SetArgs setArgs) {
        return tx.enqueue(reactive._set(key, value, setArgs));
    }

    @Override
    public Uni<Void> setGet(K key, V value) {
        return tx.enqueue(reactive._setGet(key, value));
    }

    @Override
    public Uni<Void> setGet(K key, V value, SetArgs setArgs) {
        return tx.enqueue(reactive._setGet(key, value, setArgs));
    }

    @Override
    public Uni<Void> setGet(K key, V value, io.quarkus.redis.datasource.string.SetArgs setArgs) {
        return setGet(key, value, (SetArgs) setArgs);
    }

    @Override
    public Uni<Void> setex(K key, long seconds, V value) {
        return tx.enqueue(reactive._setex(key, seconds, value));
    }

    @Override
    public Uni<Void> setnx(K key, V value) {
        return tx.enqueue(reactive._setnx(key, value));
    }

    @Override
    public Uni<Void> setrange(K key, long offset, V value) {
        return tx.enqueue(reactive._setrange(key, offset, value));
    }

    @Override
    public Uni<Void> strlen(K key) {
        return tx.enqueue(reactive._strlen(key));
    }

}
