package io.quarkus.redis.datasource.impl;

import io.quarkus.redis.datasource.api.hyperloglog.ReactiveHyperLogLogCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveHyperLogLogCommandsImpl<K, V> extends AbstractHyperLogLogCommands<K, V>
        implements ReactiveHyperLogLogCommands<K, V> {

    public ReactiveHyperLogLogCommandsImpl(RedisCommandExecutor redis, Class<K> k, Class<V> v) {
        super(redis, k, v);
    }

    @SafeVarargs
    @Override
    public final Uni<Boolean> pfadd(K key, V... values) {
        return super._pfadd(key, values)
                .map(Response::toBoolean);
    }

    @SafeVarargs
    @Override
    public final Uni<Void> pfmerge(K destination, K... sources) {
        return super._pfmerge(destination, sources)
                .replaceWithVoid();
    }

    @SafeVarargs
    @Override
    public final Uni<Long> pfcount(K... keys) {
        return super._pfcount(keys)
                .map(Response::toLong);
    }

}
