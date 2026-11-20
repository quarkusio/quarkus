package io.quarkus.redis.lettuce.runtime.internal.hyperloglog;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.hyperloglog.ReactiveHyperLogLogCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnectionSource;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;

public class LettuceReactiveHyperLogLogCommandsImpl<K, V> extends AbstractLettuceCommands<K, V>
        implements ReactiveHyperLogLogCommands<K, V> {

    public LettuceReactiveHyperLogLogCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnectionSource source, Type keyType, Type valueType) {
        super(dataSource, source, keyType, valueType, new Marshaller(keyType, valueType));
    }

    @SafeVarargs
    @Override
    public final Uni<Boolean> pfadd(K key, V... values) {
        return _pfadd(key, values).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Boolean> _pfadd(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        return command(async -> async.pfadd(marshaller.encode(key), marshaller.encodeAsArray(values)),
                AbstractLettuceCommands::toBoolean);
    }

    @SafeVarargs
    @Override
    public final Uni<Void> pfmerge(K destkey, K... sourcekeys) {
        return _pfmerge(destkey, sourcekeys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<String, Void> _pfmerge(K destkey, K... sourcekeys) {
        nonNull(destkey, "destination");
        notNullOrEmpty(sourcekeys, "sources");
        doesNotContainNull(sourcekeys, "sources");
        return discarding(async -> async.pfmerge(marshaller.encode(destkey), marshaller.encodeAsArray(sourcekeys)));
    }

    @SafeVarargs
    @Override
    public final Uni<Long> pfcount(K... keys) {
        return _pfcount(keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<Long, Long> _pfcount(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return command(async -> async.pfcount(marshaller.encodeAsArray(keys)));
    }

}
