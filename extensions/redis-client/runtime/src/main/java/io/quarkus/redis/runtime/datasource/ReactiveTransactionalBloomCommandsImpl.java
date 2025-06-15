package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.ReactiveTransactionalBloomCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalBloomCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalBloomCommands<K, V> {

    private final ReactiveBloomCommandsImpl<K, V> reactive;

    public ReactiveTransactionalBloomCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveBloomCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> bfadd(K key, V value) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._bfadd(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bfexists(K key, V value) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._bfexists(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bfmadd(K key, V... values) {
        this.tx.enqueue(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
        return this.reactive._bfmadd(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bfmexists(K key, V... values) {
        this.tx.enqueue(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
        return this.reactive._bfmexists(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity) {
        return bfreserve(key, errorRate, capacity, new BfReserveArgs());
    }

    @Override
    public Uni<Void> bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        this.tx.enqueue(r -> null);
        return this.reactive._bfreserve(key, errorRate, capacity, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> bfinsert(K key, BfInsertArgs args, V... values) {
        this.tx.enqueue(ReactiveBloomCommandsImpl::decodeAsListOfBooleans);
        return this.reactive._bfinsert(key, args, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
