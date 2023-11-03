package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrEmpty;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.positive;
import static io.smallrye.mutiny.helpers.ParameterValidation.positiveOrZero;

import java.lang.reflect.Type;
import java.util.Map;

import io.quarkus.redis.datasource.string.GetExArgs;
import io.quarkus.redis.datasource.string.SetArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

class AbstractStringCommands<K, V> extends AbstractRedisCommands {

    protected final Type typeOfValue;

    AbstractStringCommands(RedisCommandExecutor redis, Type k, Type v) {
        super(redis, new Marshaller(k, v));
        this.typeOfValue = v;
    }

    Uni<Response> _set(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        RedisCommand cmd = RedisCommand.of(Command.SET)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value));
        return execute(cmd);
    }

    Uni<Response> _set(K key, V value, SetArgs setArgs) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(setArgs, "setArgs");
        RedisCommand cmd = RedisCommand.of(Command.SET);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(value));
        cmd.putArgs(setArgs);
        return execute(cmd);
    }

    Uni<Response> _set(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(setArgs, "setArgs");
        RedisCommand cmd = RedisCommand.of(Command.SET);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(value));
        cmd.putArgs(setArgs);
        return execute(cmd);
    }

    Uni<Response> _setGet(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        RedisCommand cmd = RedisCommand.of(Command.SET);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(value));
        cmd.putArgs(new SetArgs().get());
        return execute(cmd);
    }

    V decodeV(Response r) {
        return marshaller.decode(typeOfValue, r);
    }

    Uni<Response> _setGet(K key, V value, SetArgs setArgs) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(setArgs, "setArgs");
        RedisCommand cmd = RedisCommand.of(Command.SET);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(value));
        cmd.putArgs(setArgs.get());
        return execute(cmd);
    }

    Uni<Response> _setGet(K key, V value, io.quarkus.redis.datasource.value.SetArgs setArgs) {
        nonNull(key, "key");
        nonNull(value, "value");
        nonNull(setArgs, "setArgs");
        RedisCommand cmd = RedisCommand.of(Command.SET);
        cmd.put(marshaller.encode(key));
        cmd.put(marshaller.encode(value));
        cmd.putArgs(setArgs.get());
        return execute(cmd);
    }

    Uni<Response> _setex(K key, long seconds, V value) {
        nonNull(key, "key");
        positive(seconds, "seconds");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.SETEX)
                .put(marshaller.encode(key))
                .put(seconds)
                .put(marshaller.encode(value)));
    }

    Uni<Response> _psetex(K key, long milliseconds, V value) {
        nonNull(key, "key");
        positive(milliseconds, "seconds");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.PSETEX)
                .put(marshaller.encode(key))
                .put(milliseconds)
                .put(marshaller.encode(value)));
    }

    Uni<Response> _setnx(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.SETNX)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value)));
    }

    Uni<Response> _setrange(K key, long offset, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        positiveOrZero(offset, "offset");
        return execute(RedisCommand.of(Command.SETRANGE)
                .put(marshaller.encode(key))
                .put(offset)
                .put(marshaller.encode(value)));
    }

    Uni<Response> _strlen(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.STRLEN)
                .put(marshaller.encode(key)));
    }

    Uni<Response> _decr(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.DECR)
                .put(marshaller.encode(key)));
    }

    Uni<Response> _decrby(K key, long amount) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.DECRBY)
                .put(marshaller.encode(key))
                .put(amount));
    }

    Uni<Response> _get(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.GET)
                .put(marshaller.encode(key)));
    }

    Uni<Response> _getdel(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.GETDEL)
                .put(marshaller.encode(key)));
    }

    Uni<Response> _getex(K key, GetExArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.GETEX);
        cmd.put(marshaller.encode(key));
        cmd.putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _getex(K key, io.quarkus.redis.datasource.value.GetExArgs args) {
        nonNull(key, "key");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.GETEX);
        cmd.put(marshaller.encode(key));
        cmd.putArgs(args);
        return execute(cmd);
    }

    Uni<Response> _getrange(K key, long start, long end) {
        nonNull(key, "key");
        positiveOrZero(start, "start");
        return execute(RedisCommand.of(Command.GETRANGE)
                .put(marshaller.encode(key))
                .put(start)
                .put(end));
    }

    Uni<Response> _getset(K key, V value) {
        nonNull(key, "key");
        nonNull(value, "value");
        return execute(RedisCommand.of(Command.GETSET)
                .put(marshaller.encode(key))
                .put(marshaller.encode(value)));
    }

    Uni<Response> _incr(K key) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.INCR)
                .put(marshaller.encode(key)));
    }

    Uni<Response> _incrby(K key, long amount) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.INCRBY)
                .put(marshaller.encode(key)).put(amount));
    }

    Uni<Response> _incrbyfloat(K key, double amount) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.INCRBYFLOAT)
                .put(marshaller.encode(key)).put(amount));
    }

    Uni<Response> _append(K key, V value) {
        nonNull(key, "key");
        return execute(RedisCommand.of(Command.APPEND)
                .put(marshaller.encode(key)).put(marshaller.encode(value)));
    }

    Uni<Response> _mget(K... keys) {
        notNullOrEmpty(keys, "keys");
        doesNotContainNull(keys, "keys");
        return execute(RedisCommand.of(Command.MGET)
                .put(marshaller.encode(keys)));
    }

    Map<K, V> decodeAsOrderedMap(Response r, K[] keys) {
        return marshaller.decodeAsOrderedMap(r, typeOfValue, keys);
    }

    Uni<Response> _mset(Map<K, V> map) {
        notNullOrEmpty(map, "map");
        RedisCommand cmd = RedisCommand.of(Command.MSET);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey())).put(marshaller.encode(entry.getValue()));
        }
        return execute(cmd);
    }

    Uni<Response> _msetnx(Map<K, V> map) {
        notNullOrEmpty(map, "map");
        RedisCommand cmd = RedisCommand.of(Command.MSETNX);
        for (Map.Entry<K, V> entry : map.entrySet()) {
            cmd.put(marshaller.encode(entry.getKey())).put(marshaller.encode(entry.getValue()));
        }
        return execute(cmd);
    }

    Uni<Response> _lcs(K key1, K key2) {
        nonNull(key1, "key1");
        nonNull(key2, "key2");

        return execute(RedisCommand.of(Command.LCS).put(marshaller.encode(key1)).put(marshaller.encode(key2)));
    }

    Uni<Response> _lcsLength(K key1, K key2) {
        nonNull(key1, "key1");
        nonNull(key2, "key2");

        return execute(RedisCommand.of(Command.LCS).put(marshaller.encode(key1)).put(marshaller.encode(key2)).put("LEN"));
    }
}
