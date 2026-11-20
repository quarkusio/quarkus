package io.quarkus.redis.lettuce.runtime.internal.autosuggest;

import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveTransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTransactionalAutoSuggestCommandsImpl<K>
        implements ReactiveTransactionalAutoSuggestCommands<K> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveAutoSuggestCommandsImpl<K> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalAutoSuggestCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveAutoSuggestCommandsImpl<K> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> ftSugAdd(K key, String string, double score, boolean increment) {
        return tx.enqueue(reactive._ftSugAdd(key, string, score, increment));
    }

    @Override
    public Uni<Void> ftSugDel(K key, String string) {
        return tx.enqueue(reactive._ftSugDel(key, string));
    }

    @Override
    public Uni<Void> ftSugget(K key, String prefix) {
        return tx.enqueue(reactive._ftSugGet(key, prefix));
    }

    @Override
    public Uni<Void> ftSugget(K key, String prefix, GetArgs args) {
        return tx.enqueue(reactive._ftSugGet(key, prefix, args));
    }

    @Override
    public Uni<Void> ftSugLen(K key) {
        return tx.enqueue(reactive._ftSugLen(key));
    }

}
