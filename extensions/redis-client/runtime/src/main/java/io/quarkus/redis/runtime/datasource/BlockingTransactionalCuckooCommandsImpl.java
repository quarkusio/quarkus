package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.quarkus.redis.datasource.cuckoo.ReactiveTransactionalCuckooCommands;
import io.quarkus.redis.datasource.cuckoo.TransactionalCuckooCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalCuckooCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalCuckooCommands<K, V> {

    private final ReactiveTransactionalCuckooCommands<K, V> reactive;

    public BlockingTransactionalCuckooCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalCuckooCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void cfadd(K key, V value) {
        reactive.cfadd(key, value).await().atMost(timeout);
    }

    @Override
    public void cfaddnx(K key, V value) {
        reactive.cfaddnx(key, value).await().atMost(timeout);
    }

    @Override
    public void cfcount(K key, V value) {
        reactive.cfcount(key, value).await().atMost(timeout);
    }

    @Override
    public void cfdel(K key, V value) {
        reactive.cfdel(key, value).await().atMost(timeout);
    }

    @Override
    public void cfexists(K key, V value) {
        reactive.cfexists(key, value).await().atMost(timeout);
    }

    @Override
    public void cfinsert(K key, V... values) {
        reactive.cfinsert(key, values).await().atMost(timeout);
    }

    @Override
    public void cfinsert(K key, CfInsertArgs args, V... values) {
        reactive.cfinsert(key, args, values).await().atMost(timeout);
    }

    @Override
    public void cfinsertnx(K key, V... values) {
        reactive.cfinsertnx(key, values).await().atMost(timeout);
    }

    @Override
    public void cfinsertnx(K key, CfInsertArgs args, V... values) {
        reactive.cfinsertnx(key, args, values).await().atMost(timeout);
    }

    @Override
    public void cfmexists(K key, V... values) {
        reactive.cfmexists(key, values).await().atMost(timeout);
    }

    @Override
    public void cfreserve(K key, long capacity) {
        reactive.cfreserve(key, capacity).await().atMost(timeout);
    }

    @Override
    public void cfreserve(K key, long capacity, CfReserveArgs args) {
        reactive.cfreserve(key, capacity, args).await().atMost(timeout);
    }
}
