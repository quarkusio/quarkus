package io.quarkus.redis.runtime.datasource;

import io.quarkus.redis.datasource.hyperloglog.ReactiveTransactionalHyperLogLogCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalHyperLogLogCommandsImpl<K, V> extends AbstractTransactionalCommands
        implements ReactiveTransactionalHyperLogLogCommands<K, V> {

    private final ReactiveHyperLogLogCommandsImpl<K, V> reactive;

    public ReactiveTransactionalHyperLogLogCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveHyperLogLogCommandsImpl<K, V> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public Uni<Void> pfadd(K key, V... values) {
        this.tx.enqueue(Response::toBoolean);
        return this.reactive._pfadd(key, values).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pfmerge(K destkey, K... sourcekeys) {
        this.tx.enqueue(resp -> null);
        return this.reactive._pfmerge(destkey, sourcekeys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> pfcount(K... keys) {
        this.tx.enqueue(Response::toLong);
        return this.reactive._pfcount(keys).invoke(this::queuedOrDiscard).replaceWithVoid();
    }
}
