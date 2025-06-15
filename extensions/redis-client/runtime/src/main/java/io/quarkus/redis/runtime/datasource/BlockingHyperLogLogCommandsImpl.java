package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;

public class BlockingHyperLogLogCommandsImpl<K, V> extends AbstractRedisCommandGroup
        implements HyperLogLogCommands<K, V> {

    private final ReactiveHyperLogLogCommands<K, V> reactive;

    public BlockingHyperLogLogCommandsImpl(RedisDataSource ds, ReactiveHyperLogLogCommands<K, V> reactive,
            Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public boolean pfadd(K key, V... values) {
        return reactive.pfadd(key, values).await().atMost(timeout);
    }

    @Override
    public void pfmerge(K destkey, K... sourcekeys) {
        reactive.pfmerge(destkey, sourcekeys).await().atMost(timeout);
    }

    @Override
    public long pfcount(K... keys) {
        return reactive.pfcount(keys).await().atMost(timeout);
    }
}
