package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.countmin.CountMinCommands;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;

public class BlockingCountMinCommandsImpl<K, V> extends AbstractRedisCommandGroup implements CountMinCommands<K, V> {

    private final ReactiveCountMinCommands<K, V> reactive;

    public BlockingCountMinCommandsImpl(RedisDataSource ds, ReactiveCountMinCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public long cmsIncrBy(K key, V value, long increment) {
        return reactive.cmsIncrBy(key, value, increment).await().atMost(timeout);
    }

    @Override
    public Map<V, Long> cmsIncrBy(K key, Map<V, Long> couples) {
        return reactive.cmsIncrBy(key, couples).await().atMost(timeout);
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
    public long cmsQuery(K key, V item) {
        return reactive.cmsQuery(key, item).await().atMost(timeout);
    }

    @Override
    public List<Long> cmsQuery(K key, V... items) {
        return reactive.cmsQuery(key, items).await().atMost(timeout);
    }

    @Override
    public void cmsMerge(K dest, List<K> src, List<Integer> weight) {
        reactive.cmsMerge(dest, src, weight).await().atMost(timeout);
    }
}
