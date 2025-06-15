package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.hyperloglog.TransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;

public class BlockingTransactionalHyperLogLogCommandsImpl<K, V> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalHyperLogLogCommands<K, V> {

    private final ReactiveTransactionalHyperLogLogCommands<K, V> reactive;

    public BlockingTransactionalHyperLogLogCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalHyperLogLogCommands<K, V> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
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
