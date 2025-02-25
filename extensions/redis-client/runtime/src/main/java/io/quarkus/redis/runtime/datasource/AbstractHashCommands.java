package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.quarkus.redis.runtime.datasource.Validation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractHashCommands<K, F, V> extends AbstractRedisCommands {

    protected final Type typeOfValue;
    protected final Type typeOfField;

    AbstractHashCommands(RedisCommandExecutor redis, Type k, Type f, Type v) {
        super(redis, new Marshaller(k, f, v));
        this.typeOfField = f;
        this.typeOfValue = v;
    }

    Uni<Response> _hdel(K key, F[] fields) {
        nonNull(key, "key");
        notNullOrEmpty(fields, "fields");
        doesNotContainNull(fields, "fields");

        RedisCommand cmd = RedisCommand.of(Command.HDEL)
                .put(marshaller.encode(key));
        for (F field : fields) {
            cmd.put(marshaller.encode(field));
        }
        return execute(cmd);
    }

    Uni<Response> _hexists(K key, F field) {
        nonNull(key, "key");
        nonNull(field, "field");
        return execute(RedisCommand.of(Command.HEXISTS).put(marshaller.encode(key)).put(marshaller.encode(field)));
    }

    Uni<Response> _hget(K key, F field) {
        nonNull(key, "key");
        nonNull(field, "field");
        return execute(RedisCommand.of(Command.HGET).put(marshaller.encode(key)).put(marshaller.encode(field)));
    }

    Uni<Response> _hincrby(K key, F field, long amount) {
        nonNull(key, "key");
        nonNull(field, "field");
        return execute(RedisCommand.of(Command.HINCRBY).put(marshaller.encode(key))
                .put(marshaller.encode(field)).put(amount));
    }

    Uni<Response> _hincrbyfloat(K key, F field, double amount) {
        nonNull(key, "key");
        nonNull(field, "field");
        return execute(RedisCommand.of(Command.HINCRBYFLOAT).put(marshaller.encode(key))
                .put(marshaller.encode(field)).put(amount));
    }

    Uni<Response> _hgetall(K key) {
        nonNull(key, "key");
        return execute((RedisCommand.of(Command.HGETALL).put(marshaller.encode(key))));
    }

    Uni<Response> _hkeys(K key) {
        nonNull(key, "key");
        return execute((RedisCommand.of(Command.HKEYS).put(marshaller.encode(key))));
    }

    Uni<Response> _hlen(K key) {
        nonNull(key, "key");
        return execute((RedisCommand.of(Command.HLEN).put(marshaller.encode(key))));
    }

    @SafeVarargs
    final Uni<Response> _hmget(K key, F... fields) {
        nonNull(key, "key");
        doesNotContainNull(fields, "fields");
        if (fields.length == 0) {
            return Uni.createFrom().failure(new IllegalArgumentException("`fields` must not be empty"));
        }
        RedisCommand cmd = RedisCommand.of(Command.HMGET);
        cmd.put(marshaller.encode(key));

        for (F field : fields) {
            cmd.put(marshaller.encode(field));
        }

        return execute(cmd);
    }

    Uni<Response> _hmset(K key, Map<F, V> map) {
        nonNull(key, "key");
        nonNull(map, "map");
        if (map.isEmpty()) {
            return Uni.createFrom().failure(new IllegalArgumentException("`map` must not be empty"));
        }
        RedisCommand cmd = RedisCommand.of(Command.HMSET);
        cmd.put(marshaller.encode(key));
        for (Map.Entry<F, V> entry : map.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey()));
            cmd.putNullable(marshaller.encode(entry.getValue()));
        }
        return execute(cmd);
    }

    Uni<Response> _hrandfield(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.HRANDFIELD).put(marshaller.encode(key)));
    }

    Uni<Response> _hrandfield(K key, long count) {
        nonNull(key, "key");
        positive(count, "count");
        return execute(RedisCommand.of(Command.HRANDFIELD).put(marshaller.encode(key)).put(count));
    }

    Uni<Response> _hrandfieldWithValues(K key, long count) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.HRANDFIELD).put(marshaller.encode(key)).put(count).put("WITHVALUES"));
    }

    Uni<Response> _hset(K key, F field, V value) {
        nonNull(key, "key");
        nonNull(field, "field");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.HSET)
                .put(marshaller.encode(key))
                .put(marshaller.encode(field))
                .put(marshaller.encode(value)));
    }

    Uni<Response> _hset(K key, Map<F, V> map) {
        nonNull(key, "key");
        nonNull(map, "map");
        if (map.isEmpty()) {
            return Uni.createFrom().failure(new IllegalArgumentException("`map` must not be empty"));
        }
        RedisCommand cmd = RedisCommand.of(Command.HSET);
        cmd.put(marshaller.encode(key));
        for (Map.Entry<F, V> entry : map.entrySet()) {
            cmd
                    .put(marshaller.encode(entry.getKey()))
                    .put(marshaller.encode(entry.getValue()));
        }
        return execute(cmd);
    }

    Uni<Response> _hsetnx(K key, F field, V value) {
        nonNull(key, "key");
        nonNull(field, "field");
        return execute(RedisCommand.of(Command.HSETNX)
                .put(marshaller.encode(key))
                .put(marshaller.encode(field))
                .put(marshaller.encode(value)));
    }

    Uni<Response> _hstrlen(K key, F field) {
        nonNull(key, "key");
        nonNull(field, "field");
        return execute(RedisCommand.of(Command.HSTRLEN).put(marshaller.encode(key))
                .put(marshaller.encode(field)));
    }

    Uni<Response> _hvals(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.HVALS).put(marshaller.encode(key)));
    }

    V decodeV(Response resp) {
        return marshaller.decode(typeOfValue, resp);
    }

    Map<F, V> decodeMap(Response r) {
        return marshaller.decodeAsMap(r, typeOfField, typeOfValue);
    }

    List<F> decodeListOfField(Response r) {
        return marshaller.decodeAsList(r, typeOfField);
    }

    List<V> decodeListOfValue(Response r) {
        return marshaller.decodeAsList(r, typeOfValue);
    }

    F decodeF(Response resp) {
        return marshaller.decode(typeOfField, resp);
    }

    Map<F, V> decodeOrderedMap(Response r, F[] fields) {
        return marshaller.decodeAsOrderedMap(r, typeOfValue, fields);
    }

    Map<F, V> decodeFieldWithValueMap(Response r) {
        if (r == null) {
            return Collections.emptyMap();
        }
        Map<F, V> map = new HashMap<>();
        for (Response nested : r) {
            map.put(marshaller.decode(typeOfField, nested.get(0)),
                    marshaller.decode(typeOfValue, nested.get(1)));
        }
        return map;
    }

}
