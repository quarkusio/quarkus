package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.TransactionalHyperLogLogCommands;

public class BlockingTransactionalHyperLogLogCommandsImpl<K, V> implements TransactionalHyperLogLogCommands<K, V> {

    private final ReactiveTransactionalHyperLogLogCommands<K, V> reactive;

    private final Duration timeout;

    public BlockingTransactionalHyperLogLogCommandsImpl(ReactiveTransactionalHyperLogLogCommands<K, V> reactive,
            Duration timeout) {
        this.reactive = reactive;
        this.timeout = timeout;
    }

    @Override
    public void pfadd(K key, V... values) {
        this.reactive.pfadd(key, values).await().atMost(this.timeout);
    }

    @Override
    public void pfmerge(K destkey, K... sourcekeys) {
        this.reactive.pfmerge(destkey, sourcekeys).await().atMost(this.timeout);
    }

    @Override
    public void pfcount(K... keys) {
        this.reactive.pfcount(keys).await().atMost(this.timeout);
    }
}
