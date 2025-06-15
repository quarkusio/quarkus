package io.quarkus.redis.runtime.datasource;

import java.util.Map;

import io.quarkus.redis.datasource.topk.ReactiveTransactionalTopKCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalTopKCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalTopKCommands<K, V> {

    private final ReactiveTopKCommandsImpl<K, V> reactive;

    public ReactiveTransactionalTopKCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveTopKCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> topkAdd(K key, V item) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive.typeOfValue).get(0)); // Uni<V>
        return this.reactive._topkAdd(key, item).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkAdd(K key, V... items) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive.typeOfValue)); // Uni<List<V>>
        return this.reactive._topkAdd(key, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkIncrBy(K key, V item, int increment) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive.typeOfValue).get(0)); // Uni<V>
        return this.reactive._topkIncrBy(key, item, increment).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkIncrBy(K key, Map<V, Integer> couples) {
        this.tx.enqueue(r -> reactive.decodeAsMapVV(couples, r)); // Uni<Map<V,V>>
        return this.reactive._topkIncrBy(key, couples).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkList(K key) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, reactive.typeOfValue)); // Uni<List<V>>
        return this.reactive._topkList(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkListWithCount(K key) {
        this.tx.enqueue(reactive::decodeAsMapVInt); // Uni<Map<V,Integer>>
        return this.reactive._topkListWithCount(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkQuery(K key, V item) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, Response::toBoolean).get(0)); // Uni<Boolean>
        return this.reactive._topkQuery(key, item).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkQuery(K key, V... items) {
        this.tx.enqueue(r -> reactive.marshaller.decodeAsList(r, Response::toBoolean)); // Uni<List<Boolean>>
        return this.reactive._topkQuery(key, items).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk) {
        this.tx.enqueue(r -> null); // Uni<Void>
        return this.reactive._topkReserve(key, topk).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk, int width, int depth, double decay) {
        this.tx.enqueue(r -> null); // Uni<Void>
        return this.reactive._topkReserve(key, topk, width, depth, decay).invoke(this::queuedOrDiscard)
                .replaceWithVoid();
    }
}
