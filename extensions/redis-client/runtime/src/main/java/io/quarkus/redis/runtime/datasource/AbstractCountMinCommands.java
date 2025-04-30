package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.isNotEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractCountMinCommands<K, V> extends AbstractRedisCommands {

    AbstractCountMinCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v));
    }

    Uni<Response> _cmsIncrBy(K key, V value, long increment) {
        // Validation
        nonNull(key, "key");
        nonNull(value, "value");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_INCRBY)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value))
                .put(increment);
        return execute(cmd);
    }

    Uni<Response> _cmsIncrBy(K key, Map<V, Long> couples) {
        // Validation
        nonNull(key, "key");
        nonNull(couples, "couples");
        if (couples.isEmpty()) {
            return Uni.createFrom().failure(new IllegalArgumentException("`couples` must not be empty"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_INCRBY)
                .put(marshaller.encode(key));
        for (Map.Entry<V, Long> entry : couples.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey()));
            cmd.put(entry.getValue());
        }

        return execute(cmd);
    }

    Uni<Response> _cmsInitByDim(K key, long width, long depth) {
        // Validation
        nonNull(key, "key");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_INITBYDIM)
                .put(marshaller.encode(key))
                .put(width)
                .put(depth);
        return execute(cmd);
    }

    Uni<Response> _cmsInitByProb(K key, double error, double probability) {
        // Validation
        nonNull(key, "key");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_INITBYPROB)
                .put(marshaller.encode(key))
                .put(error)
                .put(probability);
        return execute(cmd);
    }

    Uni<Response> _cmsQuery(K key, V item) {
        // Validation
        nonNull(key, "key");
        nonNull(item, "item");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_QUERY)
                .put(marshaller.encode(key))
                .put(marshaller.encode(item));
        return execute(cmd);
    }

    Uni<Response> _cmsQuery(K key, V... items) {
        // Validation
        nonNull(key, "key");
        doesNotContainNull(items, "items");
        if (items.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`items` must not be empty"));
        }
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_QUERY)
                .put(marshaller.encode(key));
        for (V item : items) {
            cmd.put(marshaller.encode(item));
        }
        return execute(cmd);
    }

    Uni<Response> _cmsMerge(K dest, List<K> src, List<Integer> weight) {
        // Validation
        nonNull(dest, "dest");
        doesNotContainNull(src, "src");
        isNotEmpty(src, "src");
        // Create command
        RedisCommand cmd = RedisCommand.of(Command.CMS_MERGE)
                .put(marshaller.encode(dest))
                .put(src.size());

        for (K k : src) {
            cmd.put(marshaller.encode(k));
        }

        if (weight != null) {
            cmd.put("WEIGHTS");
            for (Integer w : weight) {
                cmd.put(w);
            }
        }

        return execute(cmd);
    }
}
