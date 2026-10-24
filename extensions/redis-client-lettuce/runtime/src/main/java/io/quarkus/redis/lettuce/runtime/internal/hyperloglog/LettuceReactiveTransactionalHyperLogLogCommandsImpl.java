package io.quarkus.redis.lettuce.runtime.internal.hyperloglog;

import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

/**
 * Lettuce-backed implementation of {@link ReactiveTransactionalHyperLogLogCommands}.
 * <p>
 * A thin transactional shell over {@link LettuceReactiveHyperLogLogCommandsImpl}. Each command reuses the
 * non-transactional command-builder seam ({@code reactive._xxx(...)}) for validation and argument
 * conversion, and hands the resulting {@link io.quarkus.redis.lettuce.runtime.internal.LettuceCommand}
 * to the {@link LettuceTransactionHolder}. The command carries the same result mapper the
 * non-transactional path applies, so {@code TransactionResult.get(index)} yields the same Java type
 * as the Vert.x backend.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public class LettuceReactiveTransactionalHyperLogLogCommandsImpl<K, V>
        implements ReactiveTransactionalHyperLogLogCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveHyperLogLogCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalHyperLogLogCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveHyperLogLogCommandsImpl<K, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @SafeVarargs
    @Override
    public final Uni<Void> pfadd(K key, V... values) {
        return tx.enqueue(reactive._pfadd(key, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> pfmerge(K destkey, K... sourcekeys) {
        return tx.enqueue(reactive._pfmerge(destkey, sourcekeys));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> pfcount(K... keys) {
        return tx.enqueue(reactive._pfcount(keys));
    }

}
