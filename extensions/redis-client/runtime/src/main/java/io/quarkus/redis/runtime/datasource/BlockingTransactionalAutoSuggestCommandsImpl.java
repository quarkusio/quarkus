package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveTransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.autosuggest.TransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalAutoSuggestCommandsImpl<K> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalAutoSuggestCommands<K> {

    private final ReactiveTransactionalAutoSuggestCommands<K> reactive;

    public BlockingTransactionalAutoSuggestCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalAutoSuggestCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void ftSugAdd(K key, String string, double score, boolean increment) {
        reactive.ftSugAdd(key, string, score, increment).await().atMost(timeout);
    }

    @Override
    public void ftSugDel(K key, String string) {
        reactive.ftSugDel(key, string).await().atMost(timeout);
    }

    @Override
    public void ftSugget(K key, String prefix) {
        reactive.ftSugget(key, prefix).await().atMost(timeout);
    }

    @Override
    public void ftSugget(K key, String prefix, GetArgs args) {
        reactive.ftSugget(key, prefix, args).await().atMost(timeout);
    }

    @Override
    public void ftSugLen(K key) {
        reactive.ftSugLen(key).await().atMost(timeout);
    }
}
