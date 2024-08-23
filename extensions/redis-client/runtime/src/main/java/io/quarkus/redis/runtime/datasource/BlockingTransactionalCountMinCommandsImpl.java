package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.countmin.ReactiveTransactionalCountMinCommands;
import io.quarkus.redis.datasource.countmin.TransactionalCountMinCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalCountMinCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalCountMinCommands<K, V> {

    private final ReactiveTransactionalCountMinCommands<K, V> reactive;

    public BlockingTransactionalCountMinCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalCountMinCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void cmsIncrBy(K key, V value, long increment) {
        reactive.cmsIncrBy(key, value, increment).await().atMost(timeout);
    }

    @Override
    public void cmsIncrBy(K key, Map<V, Long> couples) {
        reactive.cmsIncrBy(key, couples).await().atMost(timeout);
    }

    @Override
    public void cmsInitByDim(K key, long width, long depth) {
        reactive.cmsInitByDim(key, width, depth).await().atMost(timeout);
    }

    @Override
    public void cmsInitByProb(K key, double error, double probability) {
        reactive.cmsInitByProb(key, error, probability).await().atMost(timeout);
    }

    @Override
    public void cmsQuery(K key, V item) {
        reactive.cmsQuery(key, item).await().atMost(timeout);
    }

    @Override
    public void cmsQuery(K key, V... items) {
        reactive.cmsQuery(key, items).await().atMost(timeout);
    }

    @Override
    public void cmsMerge(K dest, List<K> src, List<Integer> weight) {
        reactive.cmsMerge(dest, src, weight).await().atMost(timeout);
    }
}
