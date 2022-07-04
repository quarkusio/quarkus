package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.hyperloglog.HyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;

public class BlockingHyperLogLogCommandsImpl<K, V> implements HyperLogLogCommands<K, V> {

    private final ReactiveHyperLogLogCommands<K, V> reactive;
    private final Duration timeout;

    public BlockingHyperLogLogCommandsImpl(ReactiveHyperLogLogCommands<K, V> reactive, Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
    }

    @Override
    public boolean pfadd(K key, V... values) {
        return reactive.pfadd(key, values)
                .await().atMost(timeout);
    }

    @Override
    public void pfmerge(K destkey, K... sourcekeys) {
        reactive.pfmerge(destkey, sourcekeys)
                .await().atMost(timeout);
    }

    @Override
    public long pfcount(K... keys) {
        return reactive.pfcount(keys)
                .await().atMost(timeout);
    }
}
