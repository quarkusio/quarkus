package io.quarkus.redis.lettuce.runtime.internal.json;

import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalJsonCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveJsonCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._jsonXxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * that command returns.
 *
 * @param <K> the key type
 */
public class LettuceReactiveTransactionalJsonCommandsImpl<K>
        implements ReactiveTransactionalJsonCommands<K> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveJsonCommandsImpl<K> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalJsonCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveJsonCommandsImpl<K> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value) {
        return tx.enqueue(reactive._jsonSet(key, path, value));
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json) {
        return tx.enqueue(reactive._jsonSet(key, path, json));
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        return tx.enqueue(reactive._jsonSet(key, path, json, args));
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json) {
        return tx.enqueue(reactive._jsonSet(key, path, json));
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        return tx.enqueue(reactive._jsonSet(key, path, json, args));
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value, JsonSetArgs args) {
        return tx.enqueue(reactive._jsonSet(key, path, value, args));
    }

    @Override
    public <T> Uni<Void> jsonGet(K key, Class<T> clazz) {
        return tx.enqueue(reactive._jsonGet(key, clazz));
    }

    @Override
    public Uni<Void> jsonGetObject(K key) {
        return tx.enqueue(reactive._jsonGetObject(key));
    }

    @Override
    public Uni<Void> jsonGetArray(K key) {
        return tx.enqueue(reactive._jsonGetArray(key));
    }

    @Override
    public Uni<Void> jsonGet(K key, String path) {
        return tx.enqueue(reactive._jsonGet(key, path));
    }

    @Override
    public Uni<Void> jsonGet(K key, String... paths) {
        return tx.enqueue(reactive._jsonGet(key, paths));
    }

    @SafeVarargs
    @Override
    public final <T> Uni<Void> jsonArrAppend(K key, String path, T... values) {
        return tx.enqueue(reactive._jsonArrAppend(key, path, values));
    }

    @Override
    public <T> Uni<Void> jsonArrIndex(K key, String path, T value, int start, int end) {
        return tx.enqueue(reactive._jsonArrIndex(key, path, value, start, end));
    }

    @SafeVarargs
    @Override
    public final <T> Uni<Void> jsonArrInsert(K key, String path, int index, T... values) {
        return tx.enqueue(reactive._jsonArrInsert(key, path, index, values));
    }

    @Override
    public Uni<Void> jsonArrLen(K key, String path) {
        return tx.enqueue(reactive._jsonArrLen(key, path));
    }

    @Override
    public <T> Uni<Void> jsonArrPop(K key, Class<T> clazz, String path, int index) {
        return tx.enqueue(reactive._jsonArrPop(key, clazz, path, index));
    }

    @Override
    public Uni<Void> jsonArrTrim(K key, String path, int start, int stop) {
        return tx.enqueue(reactive._jsonArrTrim(key, path, start, stop));
    }

    @Override
    public Uni<Void> jsonClear(K key, String path) {
        return tx.enqueue(reactive._jsonClear(key, path));
    }

    @Override
    public Uni<Void> jsonDel(K key, String path) {
        return tx.enqueue(reactive._jsonDel(key, path));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> jsonMget(String path, K... keys) {
        return tx.enqueue(reactive._jsonMget(path, keys));
    }

    @Override
    public Uni<Void> jsonNumincrby(K key, String path, double value) {
        return tx.enqueue(reactive._jsonNumincrby(key, path, value));
    }

    @Override
    public Uni<Void> jsonObjKeys(K key, String path) {
        return tx.enqueue(reactive._jsonObjKeys(key, path));
    }

    @Override
    public Uni<Void> jsonObjLen(K key, String path) {
        return tx.enqueue(reactive._jsonObjLen(key, path));
    }

    @Override
    public Uni<Void> jsonStrAppend(K key, String path, String value) {
        return tx.enqueue(reactive._jsonStrAppend(key, path, value));
    }

    @Override
    public Uni<Void> jsonStrLen(K key, String path) {
        return tx.enqueue(reactive._jsonStrLen(key, path));
    }

    @Override
    public Uni<Void> jsonToggle(K key, String path) {
        return tx.enqueue(reactive._jsonToggle(key, path));
    }

    @Override
    public Uni<Void> jsonType(K key, String path) {
        return tx.enqueue(reactive._jsonType(key, path));
    }

}
