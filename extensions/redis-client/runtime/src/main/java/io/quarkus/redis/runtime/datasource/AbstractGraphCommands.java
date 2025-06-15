package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.time.Duration;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractGraphCommands<K> extends AbstractRedisCommands {

    AbstractGraphCommands(RedisCommandExecutor redis, Type k) {
        super(redis, new Marshaller(k));
    }

    Uni<Response> _graphDelete(K key) {
        // Validation
        nonNull(key, "key");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.GRAPH_DELETE).put(marshaller.encode(key));
        return execute(cmd);
    }

    Uni<Response> _graphExplain(K key, String query) {
        // Validation
        nonNull(key, "key");
        nonNull(query, "query");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.GRAPH_EXPLAIN).put(marshaller.encode(key)).put(query);
        return execute(cmd);
    }

    Uni<Response> _graphList() {
        RedisCommand cmd = RedisCommand.of(Command.GRAPH_LIST);
        return execute(cmd);
    }

    Uni<Response> _graphQuery(K key, String query) {

        // Validation
        nonNull(key, "key");
        nonNull(query, "query");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.GRAPH_QUERY).put(marshaller.encode(key)).put(query);
        return execute(cmd);
    }

    Uni<Response> _graphQuery(K key, String query, Duration timeout) {
        // Validation
        nonNull(key, "key");
        nonNull(query, "query");
        nonNull(timeout, "timeout");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.GRAPH_QUERY).put(marshaller.encode(key)).put(query).put("TIMEOUT")
                .put(timeout.toMillis());
        return execute(cmd);
    }
}
