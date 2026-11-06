package io.quarkus.redis.lettuce.runtime.internal.countmin;

import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTransactionalCountMinCommandsImpl<K, V>
        implements ReactiveTransactionalCountMinCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveCountMinCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalCountMinCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveCountMinCommandsImpl<K, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> cmsIncrBy(K key, V value, long increment) {
        return tx.enqueue(reactive._cmsIncrBy(key, value, increment));
    }

    @Override
    public Uni<Void> cmsIncrBy(K key, Map<V, Long> couples) {
        // The Vert.x backend records the raw List<Long> of counts in the TransactionResult, not the Map the
        // non-transactional API returns. Keep the validation and encoding, but drop the map-building mapper.
        return tx.enqueue(LettuceCommand.of(reactive._cmsIncrBy(key, couples).call()));
    }

    @Override
    public Uni<Void> cmsInitByDim(K key, long width, long depth) {
        return tx.enqueue(reactive._cmsInitByDim(key, width, depth));
    }

    @Override
    public Uni<Void> cmsInitByProb(K key, double error, double probability) {
        return tx.enqueue(reactive._cmsInitByProb(key, error, probability));
    }

    @Override
    public Uni<Void> cmsQuery(K key, V item) {
        return tx.enqueue(reactive._cmsQuery(key, item));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cmsQuery(K key, V... items) {
        return tx.enqueue(reactive._cmsQuery(key, items));
    }

    @Override
    public Uni<Void> cmsMerge(K dest, List<K> src, List<Integer> weight) {
        return tx.enqueue(reactive._cmsMerge(dest, src, weight));
    }

}
