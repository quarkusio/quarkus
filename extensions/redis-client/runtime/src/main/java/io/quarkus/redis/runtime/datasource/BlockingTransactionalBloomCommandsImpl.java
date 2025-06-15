package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.quarkus.redis.datasource.bloom.ReactiveTransactionalBloomCommands;
import io.quarkus.redis.datasource.bloom.TransactionalBloomCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalBloomCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalBloomCommands<K, V> {

    private final ReactiveTransactionalBloomCommands<K, V> reactive;

    public BlockingTransactionalBloomCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalBloomCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void bfadd(K key, V value) {
        this.reactive.bfadd(key, value).await().atMost(this.timeout);
    }

    @Override
    public void bfexists(K key, V value) {
        this.reactive.bfexists(key, value).await().atMost(this.timeout);
    }

    @Override
    public void bfmadd(K key, V... values) {
        this.reactive.bfmadd(key, values).await().atMost(this.timeout);
    }

    @Override
    public void bfmexists(K key, V... values) {
        this.reactive.bfmexists(key, values).await().atMost(this.timeout);
    }

    @Override
    public void bfreserve(K key, double errorRate, long capacity) {
        this.reactive.bfreserve(key, errorRate, capacity).await().atMost(this.timeout);
    }

    @Override
    public void bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        this.reactive.bfreserve(key, errorRate, capacity, args).await().atMost(this.timeout);
    }

    @Override
    public void bfinsert(K key, BfInsertArgs args, V... values) {
        this.reactive.bfinsert(key, args, values).await().atMost(this.timeout);
    }
}
