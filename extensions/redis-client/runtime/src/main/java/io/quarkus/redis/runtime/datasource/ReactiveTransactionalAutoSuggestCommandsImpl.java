package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.autosuggest.GetArgs;
import io.quarkus.redis.datasource.autosuggest.ReactiveTransactionalAutoSuggestCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalAutoSuggestCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalAutoSuggestCommands<K> {

    private final ReactiveAutoSuggestCommandsImpl<K> reactive;

    public ReactiveTransactionalAutoSuggestCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveAutoSuggestCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> ftSugAdd(K key, String string, double score, boolean increment) {
        this.tx.enqueue(Response::toLong); // Uni<Long>
        return this.reactive._ftSugAdd(key, string, score, increment).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSugDel(K key, String string) {
        this.tx.enqueue(Response::toBoolean); // Uni<Boolean>
        return this.reactive._ftSugDel(key, string).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSugget(K key, String prefix) {
        this.tx.enqueue(r -> reactive.decodeAsListOfSuggestion(r, false));
        return this.reactive._ftSugget(key, prefix).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSugget(K key, String prefix, GetArgs args) {
        this.tx.enqueue(r -> reactive.decodeAsListOfSuggestion(r, args.hasScores()));
        return this.reactive._ftSugget(key, prefix, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> ftSugLen(K key) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._ftSugLen(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
