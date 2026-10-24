package io.quarkus.redis.lettuce.runtime.internal.bloom;

import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.ReactiveTransactionalBloomCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalBloomCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveBloomCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._xxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * as the Vert.x backend.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LettuceReactiveTransactionalBloomCommandsImpl<K, V> implements ReactiveTransactionalBloomCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveBloomCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalBloomCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveBloomCommandsImpl<K, V> reactive,
            LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> bfadd(K key, V value) {
        return tx.enqueue(reactive._bfadd(key, value));
    }

    @Override
    public Uni<Void> bfexists(K key, V value) {
        return tx.enqueue(reactive._bfexists(key, value));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bfmadd(K key, V... values) {
        return tx.enqueue(reactive._bfmadd(key, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bfmexists(K key, V... values) {
        return tx.enqueue(reactive._bfmexists(key, values));
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity) {
        return tx.enqueue(reactive._bfreserve(key, errorRate, capacity, new BfReserveArgs()));
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        return tx.enqueue(reactive._bfreserve(key, errorRate, capacity, args));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> bfinsert(K key, BfInsertArgs args, V... values) {
        return tx.enqueue(reactive._bfinsert(key, args, values));
    }

}
