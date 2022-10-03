package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.ReactiveTransactionalCuckooCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalCuckooCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalCuckooCommands<K, V> {

    private final ReactiveCuckooCommandsImpl<K, V> reactive;

    public ReactiveTransactionalCuckooCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveCuckooCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> cfadd(K key, V value) {
        this.tx.enqueue(r -> null); // Uni<Void>
        return this.reactive._cfadd(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfaddnx(K key, V value) {
        this.tx.enqueue(Response::toBoolean); // Uni<Boolean>
        return this.reactive._cfaddnx(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfcount(K key, V value) {
        this.tx.enqueue(Response::toLong); // Uni<Long>
        return this.reactive._cfcount(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfdel(K key, V value) {
        this.tx.enqueue(Response::toBoolean); // Uni<Boolean>
        return this.reactive._cfdel(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfexists(K key, V value) {
        this.tx.enqueue(Response::toBoolean); // Uni<Boolean>
        return this.reactive._cfexists(key, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfinsert(K key, V... values) {
        this.tx.enqueue(r -> null);
        return this.reactive._cfinsert(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfinsert(K key, CfInsertArgs args, V... values) {
        this.tx.enqueue(r -> null);
        return this.reactive._cfinsert(key, args, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfinsertnx(K key, V... values) {
        this.tx.enqueue(ReactiveBloomCommandsImpl::decodeAsListOfBooleans); // Uni<List<Boolean>>
        return this.reactive._cfinsertnx(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfinsertnx(K key, CfInsertArgs args, V... values) {
        this.tx.enqueue(ReactiveBloomCommandsImpl::decodeAsListOfBooleans); // Uni<List<Boolean>>
        return this.reactive._cfinsertnx(key, args, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfmexists(K key, V... values) {
        this.tx.enqueue(ReactiveBloomCommandsImpl::decodeAsListOfBooleans); // Uni<List<Boolean>>
        return this.reactive._cfmexists(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity) {
        this.tx.enqueue(r -> null); // Uni<Void>
        return this.reactive._cfreserve(key, capacity).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity, CfReserveArgs args) {
        this.tx.enqueue(r -> null); // Uni<Void>
        return this.reactive._cfreserve(key, capacity, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
