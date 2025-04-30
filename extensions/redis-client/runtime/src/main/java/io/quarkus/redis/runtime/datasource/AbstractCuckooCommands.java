package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;

import io.quarkus.redis.datasource.cuckoo.CfInsertArgs;
import io.quarkus.redis.datasource.cuckoo.CfReserveArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractCuckooCommands<K, V> extends AbstractRedisCommands {

    AbstractCuckooCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v));
    }

    Uni<Response> _cfadd(K key, V value) {
        // Validation
        nonNull(key, "key");
        nonNull(value, "value");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_ADD)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));

        return execute(cmd);
    }

    Uni<Response> _cfaddnx(K key, V value) {
        // Validation
        nonNull(key, "key");
        nonNull(value, "value");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_ADDNX)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));
        return execute(cmd);
    }

    Uni<Response> _cfcount(K key, V value) {
        // Validation
        nonNull(key, "key");
        nonNull(value, "value");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_COUNT)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));
        return execute(cmd);
    }

    Uni<Response> _cfdel(K key, V value) {
        // Validation
        nonNull(key, "key");
        nonNull(value, "value");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_DEL)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));
        return execute(cmd);
    }

    Uni<Response> _cfexists(K key, V value) {
        // Validation
        nonNull(key, "key");
        nonNull(value, "value");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_EXISTS)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));
        return execute(cmd);
    }

    Uni<Response> _cfinsert(K key, V... values) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }

        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_INSERT)
                .put(marshaller.encode(key));
        for (V value : values) {
            cmd.put(marshaller.encode(value));
        }
        return execute(cmd);
    }

    Uni<Response> _cfinsert(K key, CfInsertArgs args, V... values) {
        // Validation
        nonNull(key, "key");
        nonNull(args, "args");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_INSERT)
                .put(marshaller.encode(key))
                .putArgs(args)
                .put("ITEMS");
        for (V value : values) {
            cmd.put(marshaller.encode(value));
        }
        return execute(cmd);
    }

    Uni<Response> _cfinsertnx(K key, V... values) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_INSERTNX)
                .put(marshaller.encode(key))
                .put("ITEMS");
        for (V value : values) {
            cmd.put(marshaller.encode(value));
        }
        return execute(cmd);
    }

    Uni<Response> _cfinsertnx(K key, CfInsertArgs args, V... values) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        nonNull(args, "args");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_INSERTNX)
                .put(marshaller.encode(key))
                .putArgs(args)
                .put("ITEMS");
        for (V value : values) {
            cmd.put(marshaller.encode(value));
        }
        return execute(cmd);
    }

    Uni<Response> _cfmexists(K key, V... values) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        if (values.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`values` must contain at least one item"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_MEXISTS)
                .put(marshaller.encode(key));
        for (V value : values) {
            cmd.put(marshaller.encode(value));
        }
        return execute(cmd);
    }

    Uni<Response> _cfreserve(K key, long capacity) {
        // Validation
        nonNull(key, "key");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_RESERVE)
                .put(marshaller.encode(key))
                .put(capacity);
        return execute(cmd);
    }

    Uni<Response> _cfreserve(K key, long capacity, CfReserveArgs args) {
        // Validation
        nonNull(key, "key");
        nonNull(args, "args");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CF_RESERVE)
                .put(marshaller.encode(key))
                .put(capacity)
                .putArgs(args);
        return execute(cmd);
    }
}
