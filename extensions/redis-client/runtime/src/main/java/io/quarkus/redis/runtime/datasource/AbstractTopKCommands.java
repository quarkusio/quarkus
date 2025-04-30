package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.isNotEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;

import java.lang.reflect.Type;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractTopKCommands<K, V> extends AbstractRedisCommands {

    AbstractTopKCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v));
    }

    Uni<Response> _topkAdd(K key, V item) {
        // Validation
        nonNull(key, "key");
        nonNull(item, "item");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_ADD)
                .put(marshaller.encode(key))
                .put(marshaller.encode(item));
        return execute(cmd);
    }

    Uni<Response> _topkAdd(K key, V... items) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(items, "items");
        if (items.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`items` must not be empty"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_ADD)
                .put(marshaller.encode(key))
                .putAll(marshaller.encode(items));
        return execute(cmd);
    }

    Uni<Response> _topkIncrBy(K key, V item, int increment) {
        // Validation
        nonNull(key, "key");
        nonNull(item, "item");
        positive(increment, "increment");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_INCRBY)
                .put(marshaller.encode(key))
                .put(marshaller.encode(item))
                .put(increment);
        return execute(cmd);
    }

    Uni<Response> _topkIncrBy(K key, Map<V, Integer> couples) {
        // Validation
        nonNull(key, "key");
        nonNull(couples, "couples");
        isNotEmpty(couples.keySet(), "couples");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_INCRBY)
                .put(marshaller.encode(key));
        for (Map.Entry<V, Integer> entry : couples.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey()));
            cmd.put(entry.getValue());
        }
        return execute(cmd);
    }

    Uni<Response> _topkList(K key) {
        // Validation
        nonNull(key, "key");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_LIST)
                .put(marshaller.encode(key));
        return execute(cmd);
    }

    Uni<Response> _topkListWithCount(K key) {
        // Validation
        nonNull(key, "key");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_LIST)
                .put(marshaller.encode(key))
                .put("WITHCOUNT");
        return execute(cmd);
    }

    Uni<Response> _topkQuery(K key, V item) {
        // Validation
        nonNull(key, "key");
        nonNull(item, "item");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_QUERY)
                .put(marshaller.encode(key))
                .put(marshaller.encode(item));
        return execute(cmd);
    }

    Uni<Response> _topkQuery(K key, V... items) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(items, "items");
        if (items.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`items` must not be empty"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_QUERY)
                .put(marshaller.encode(key))
                .putAll(marshaller.encode(items));
        return execute(cmd);
    }

    Uni<Response> _topkReserve(K key, int topk) {
        // Validation
        nonNull(key, "key");
        positive(topk, "topk");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_RESERVE)
                .put(marshaller.encode(key))
                .put(topk);

        return execute(cmd);
    }

    Uni<Response> _topkReserve(K key, int topk, int width, int depth, double decay) {
        // Validation
        nonNull(key, "key");
        positive(topk, "topk");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.TOPK_RESERVE)
                .put(marshaller.encode(key))
                .put(topk)
                .put(width)
                .put(depth)
                .put(decay);
        return execute(cmd);
    }
}
