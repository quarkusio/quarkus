package io.quarkus.redis.lettuce.runtime.internal.topk;

import java.util.Map;

import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTransactionalTopKCommandsImpl<K, V>
        implements ReactiveTransactionalTopKCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveTopKCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalTopKCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveTopKCommandsImpl<K, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> topkAdd(K key, V item) {
        return tx.enqueue(reactive._topkAdd(key, item));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> topkAdd(K key, V... items) {
        return tx.enqueue(reactive._topkAdd(key, items));
    }

    @Override
    public Uni<Void> topkIncrBy(K key, V item, int increment) {
        return tx.enqueue(reactive._topkIncrBy(key, item, increment));
    }

    @Override
    public Uni<Void> topkIncrBy(K key, Map<V, Integer> couples) {
        return tx.enqueue(reactive._topkIncrBy(key, couples));
    }

    @Override
    public Uni<Void> topkList(K key) {
        return tx.enqueue(reactive._topkList(key));
    }

    @Override
    public Uni<Void> topkListWithCount(K key) {
        return tx.enqueue(reactive._topkListWithCount(key));
    }

    @Override
    public Uni<Void> topkQuery(K key, V item) {
        return tx.enqueue(reactive._topkQuery(key, item));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> topkQuery(K key, V... items) {
        return tx.enqueue(reactive._topkQuery(key, items));
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk) {
        return tx.enqueue(reactive._topkReserve(key, topk));
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk, int width, int depth, double decay) {
        return tx.enqueue(reactive._topkReserve(key, topk, width, depth, decay));
    }

}
