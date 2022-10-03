package io.quarkus.redis.runtime.datasource;

import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;

public class ReactiveTransactionalCountMinCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalCountMinCommands<K, V> {

    private final ReactiveCountMinCommandsImpl<K, V> reactive;

    public ReactiveTransactionalCountMinCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveCountMinCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> cmsIncrBy(K key, V value, long increment) {
        this.tx.enqueue(r -> r.get(0).toLong()); // Uni<Long>
        return this.reactive._cmsIncrBy(key, value, increment).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsIncrBy(K key, Map<V, Long> couples) {
        this.tx.enqueue(reactive::decodeAListOfLongs);
        return this.reactive._cmsIncrBy(key, couples).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsInitByDim(K key, long width, long depth) {
        this.tx.enqueue(r -> null);
        return this.reactive._cmsInitByDim(key, width, depth).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsInitByProb(K key, double error, double probability) {
        this.tx.enqueue(r -> null);
        return this.reactive._cmsInitByProb(key, error, probability).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsQuery(K key, V item) {
        this.tx.enqueue(r -> reactive.decodeAListOfLongs(r).get(0));
        return this.reactive._cmsQuery(key, item).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsQuery(K key, V... items) {
        this.tx.enqueue(reactive::decodeAListOfLongs);
        return this.reactive._cmsQuery(key, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsMerge(K dest, List<K> src, List<Integer> weight) {
        this.tx.enqueue(r -> null);
        return this.reactive._cmsMerge(dest, src, weight).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
