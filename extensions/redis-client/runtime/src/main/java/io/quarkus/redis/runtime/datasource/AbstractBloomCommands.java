package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;

import io.quarkus.redis.datasource.bloom.BfInsertArgs;
import io.quarkus.redis.datasource.bloom.BfReserveArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractBloomCommands<K, V> extends AbstractRedisCommands {

    AbstractBloomCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v));
    }

    Uni<Response> _bfadd(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");

        RedisCommand command = RedisCommand.of(Command.BF_ADD)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));

        return execute(command);
    }

    Uni<Response> _bfexists(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");

        RedisCommand command = RedisCommand.of(Command.BF_EXISTS)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));

        return execute(command);
    }

    Uni<Response> _bfmadd(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }

        RedisCommand command = RedisCommand.of(Command.BF_MADD)
                .put(marshaller.encode(key));

        for (V value : values) {
            command.put(marshaller.encode(value));
        }

        return execute(command);
    }

    Uni<Response> _bfmexists(K key, V... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }

        RedisCommand command = RedisCommand.of(Command.BF_MEXISTS)
                .put(marshaller.encode(key));

        for (V value : values) {
            command.put(marshaller.encode(value));
        }

        return execute(command);
    }

    Uni<Response> _bfreserve(K key, double errorRate, long capacity, BfReserveArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");

        RedisCommand command = RedisCommand.of(Command.BF_RESERVE)
                .put(marshaller.encode(key))
                .put(errorRate)
                .put(capacity)
                .putArgs(args);

        return execute(command);
    }

    Uni<Response> _bfinsert(K key, BfInsertArgs args, V... values) {
        nonNull(key, "key");
        nonNull(args, "args");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }

        RedisCommand command = RedisCommand.of(Command.BF_INSERT)
                .put(marshaller.encode(key))
                .putArgs(args);

        command.put("ITEMS");
        for (V value : values) {
            command.put(marshaller.encode(value));
        }

        return execute(command);
    }
}
