package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveHyperLogLogCommandsImpl<K, V> extends AbstractHyperLogLogCommands<K, V>
        implements ReactiveHyperLogLogCommands<K, V> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveHyperLogLogCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
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
