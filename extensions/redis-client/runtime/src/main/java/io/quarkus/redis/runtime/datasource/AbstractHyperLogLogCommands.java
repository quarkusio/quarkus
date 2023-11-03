package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractHyperLogLogCommands<K, V> extends AbstractRedisCommands {

    AbstractHyperLogLogCommands(RedisCommandExecutor api, Type k, Type v) {
        super(api, new Marshaller(k, v));
    }

    Uni<Response> _pfadd(K key, V... values) {
        nonNull(key, "key");
        notNullOrEmpty(values, "values");
        doesNotContainNull(values, "values");
        RedisCommand cmd = RedisCommand.of(Command.PFADD)
                .put(marshaller.encode(key))
                .putAll(marshaller.encode(values));
        return execute(cmd);
    }

    Uni<Response> _pfmerge(K destination, K... sources) {
        nonNull(destination, "destination");
        notNullOrEmpty(sources, "sources");
        doesNotContainNull(sources, "sources");
        RedisCommand cmd = RedisCommand.of(Command.PFMERGE)
                .put(marshaller.encode(destination))
                .putAll(marshaller.encode(sources));
        return execute(cmd);
    }

    Uni<Response> _pfcount(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return execute(RedisCommand.of(Command.PFCOUNT).put(marshaller.encode(keys)));
    }

}
