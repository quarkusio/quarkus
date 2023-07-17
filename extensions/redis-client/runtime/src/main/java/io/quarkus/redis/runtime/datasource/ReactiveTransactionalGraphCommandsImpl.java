package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.graph.ReactiveTransactionalGraphCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalGraphCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalGraphCommands<K> {

    private final ReactiveGraphCommandsImpl<K> reactive;

    public ReactiveTransactionalGraphCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveGraphCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> graphDelete(K key) {
        this.tx.enqueue(x -> null);
        return this.reactive._graphDelete(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> graphExplain(K key, String query) {
        this.tx.enqueue(Response::toString);
        return this.reactive._graphExplain(key, query).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> graphList() {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive.typeOfKey));
        return this.reactive._graphList().invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> graphQuery(K key, String query) {
        this.tx.enqueue(ReactiveGraphCommandsImpl::decodeQueryResponse); // Uni<List<Map<String,GraphQueryResponseItem>>>
        return this.reactive._graphQuery(key, query).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> graphQuery(K key, String query, Duration timeout) {
        this.tx.enqueue(ReactiveGraphCommandsImpl::decodeQueryResponse); // Uni<List<Map<String,GraphQueryResponseItem>>>
        return this.reactive._graphQuery(key, query, timeout).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
