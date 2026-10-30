package io.quarkus.redis.lettuce.runtime.internal.cuckoo;

import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.ReactiveTransactionalCuckooCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.quarkus.redis.lettuce.runtime.internal.datasource.LettuceTransactionHolder;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveTransactionalCuckooCommandsImpl<K, V>
        implements ReactiveTransactionalCuckooCommands<K, V> {

    private final ReactiveTransactionalRedisDataSource dataSource;
    private final LettuceReactiveCuckooCommandsImpl<K, V> reactive;
    private final LettuceTransactionHolder tx;

    public LettuceReactiveTransactionalCuckooCommandsImpl(ReactiveTransactionalRedisDataSource dataSource,
            LettuceReactiveCuckooCommandsImpl<K, V> reactive, LettuceTransactionHolder tx) {
        this.dataSource = dataSource;
        this.reactive = reactive;
        this.tx = tx;
    }

    @Override
    public ReactiveTransactionalRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Uni<Void> cfadd(K key, V value) {
        return tx.enqueue(reactive._cfadd(key, value));
    }

    @Override
    public Uni<Void> cfaddnx(K key, V value) {
        return tx.enqueue(reactive._cfaddnx(key, value));
    }

    @Override
    public Uni<Void> cfcount(K key, V value) {
        return tx.enqueue(reactive._cfcount(key, value));
    }

    @Override
    public Uni<Void> cfdel(K key, V value) {
        return tx.enqueue(reactive._cfdel(key, value));
    }

    @Override
    public Uni<Void> cfexists(K key, V value) {
        return tx.enqueue(reactive._cfexists(key, value));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfinsert(K key, V... values) {
        return tx.enqueue(reactive._cfinsert(key, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfinsert(K key, CfInsertArgs args, V... values) {
        return tx.enqueue(reactive._cfinsert(key, args, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfinsertnx(K key, V... values) {
        return tx.enqueue(reactive._cfinsertnx(key, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfinsertnx(K key, CfInsertArgs args, V... values) {
        return tx.enqueue(reactive._cfinsertnx(key, args, values));
    }

    @SafeVarargs
    @Override
    public final Uni<Void> cfmexists(K key, V... values) {
        return tx.enqueue(reactive._cfmexists(key, values));
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity) {
        return tx.enqueue(reactive._cfreserve(key, capacity));
    }

    @Override
    public Uni<Void> cfreserve(K key, long capacity, CfReserveArgs args) {
        return tx.enqueue(reactive._cfreserve(key, capacity, args));
    }

}
