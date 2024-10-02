package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Response;

public class AbstractJsonCommands<K> extends AbstractRedisCommands {
    private static final JsonSetArgs JSON_SET_DEFAULT = new JsonSetArgs();

    public AbstractJsonCommands(RedisCommandExecutor api, Type k) {
        super(api, new Marshaller(k));
    }

    <T> Uni<Response> _jsonSet(K key, String path, T value) {
        return _jsonSet(key, path, Json.encode(value).getBytes(), JSON_SET_DEFAULT);
    }

    Uni<Response> _jsonSet(K key, String path, JsonObject json) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.toBuffer().getBytes(), JSON_SET_DEFAULT);
    }

    Uni<Response> _jsonSet(K key, String path, JsonArray json) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.toBuffer().getBytes(), JSON_SET_DEFAULT);
    }

    Uni<Response> _jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.toBuffer().getBytes(), args);
    }

    Uni<Response> _jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.toBuffer().getBytes(), args);
    }

    <T> Uni<Response> _jsonSet(K key, String path, T value, JsonSetArgs args) {
        return _jsonSet(key, path, Json.encode(value).getBytes(), args);
    }

    Uni<Response> _jsonSet(K key, String path, byte[] encoded, JsonSetArgs args) {
        nonNull(key, "key");
        notNullOrBlank(path, "path");
        nonNull(args, "args");
        RedisCommand cmd = RedisCommand.of(Command.JSON_SET)
                .put(marshaller.encode(key))
                .put(path)
                .put(encoded)
                .putAll(args.toArgs());
        return execute(cmd);
    }

    Uni<Response> _jsonGet(K key) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of((Command.JSON_GET))
                .put(marshaller.encode(key));
        return execute(cmd);
    }

    Uni<Response> _jsonGet(K key, String path) {
        nonNull(key, "key");
        nonNull(path, "path");
        RedisCommand cmd = RedisCommand.of((Command.JSON_GET))
                .put(marshaller.encode(key))
                .put(path);
        return execute(cmd);
    }

    Uni<Response> _jsonGet(K key, String... paths) {
        nonNull(key, "key");
        doesNotContainNull(paths, "path");
        RedisCommand cmd = RedisCommand.of((Command.JSON_GET))
                .put(marshaller.encode(key))
                .putAll(paths);
        return execute(cmd);
    }

    <T> Uni<Response> _jsonArrAppend(K key, String path, T... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        List<String> encoded = new ArrayList<>();
        for (T value : values) {
            encoded.add(Json.encode(value));
        }
        RedisCommand cmd = RedisCommand.of((Command.JSON_ARRAPPEND))
                .put(marshaller.encode(key));

        if (path != null) {
            cmd.put(path);
        }
        cmd.putAll(encoded);
        return execute(cmd);
    }

    <T> Uni<Response> _jsonArrIndex(K key, String path, T value, long start, long end) {
        nonNull(key, "key");
        nonNull(path, "path");
        nonNull(value, "value");
        RedisCommand cmd = RedisCommand.of(Command.JSON_ARRINDEX)
                .put(marshaller.encode(key))
                .put(path)
                .put(Json.encode(value))
                .put(start)
                .put(end);
        return execute(cmd);
    }

    <T> Uni<Response> _jsonArrInsert(K key, String path, int index, T[] values) {
        nonNull(key, "key");
        nonNull(path, "path");
        doesNotContainNull(values, "values");
        RedisCommand cmd = RedisCommand.of(Command.JSON_ARRINSERT)
                .put(marshaller.encode(key))
                .put(path)
                .put(index);

        for (T value : values) {
            cmd.put(Json.encode(value));
        }

        return execute(cmd);
    }

    Uni<Response> _jsonArrLen(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of((Command.JSON_ARRLEN))
                .put(marshaller.encode(key));
        if (path != null) {
            cmd.put(path);
        }
        return execute(cmd);
    }

    Uni<Response> _jsonArrPop(K key, String path, int index) {
        nonNull(key, "key");

        RedisCommand cmd = RedisCommand.of(Command.JSON_ARRPOP)
                .put(marshaller.encode(key));

        if (path != null) {
            cmd.put(path);
        }
        if (index != -1) {
            cmd.put(-1);
        }
        return execute(cmd);
    }

    Uni<Response> _jsonArrTrim(K key, String path, int start, int stop) {
        nonNull(key, "key");
        nonNull(path, "path");
        RedisCommand cmd = RedisCommand.of(Command.JSON_ARRTRIM)
                .put(marshaller.encode(key))
                .put(path)
                .put(start)
                .put(stop);

        return execute(cmd);
    }

    Uni<Response> _jsonClear(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.JSON_CLEAR)
                .put(marshaller.encode(key));

        if (path != null) {
            cmd.put(path);
        }
        return execute(cmd);
    }

    Uni<Response> _jsonDel(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.JSON_DEL)
                .put(marshaller.encode(key));

        if (path != null) {
            cmd.put(path);
        }
        return execute(cmd);
    }

    Uni<Response> _jsonMget(String path, K... keys) {
        notNullOrBlank(path, "path");
        doesNotContainNull(keys, "keys");

        RedisCommand cmd = RedisCommand.of(Command.JSON_MGET);

        for (K key : keys) {
            cmd.put(marshaller.encode(key));
        }
        cmd.put(path);

        return execute(cmd);
    }

    Uni<Response> _jsonNumincrby(K key, String path, double value) {
        nonNull(key, "key");
        notNullOrBlank(path, "path");

        RedisCommand cmd = RedisCommand.of(Command.JSON_NUMINCRBY)
                .put(marshaller.encode(key))
                .put(path)
                .put(value);

        return execute(cmd);
    }

    Uni<Response> _jsonObjKeys(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.JSON_OBJKEYS)
                .put(marshaller.encode(key));
        if (path != null) {
            cmd.put(path);
        }

        return execute(cmd);
    }

    Uni<Response> _jsonObjLen(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.JSON_OBJLEN)
                .put(marshaller.encode(key));
        if (path != null) {
            cmd.put(path);
        }

        return execute(cmd);
    }

    Uni<Response> _jsonStrAppend(K key, String path, String value) {
        nonNull(key, "key");
        nonNull(value, "value");
        RedisCommand cmd = RedisCommand.of(Command.JSON_STRAPPEND)
                .put(marshaller.encode(key));
        if (path != null) {
            cmd.put(path);
        }
        cmd.put(Json.encode(value));
        return execute(cmd);
    }

    Uni<Response> _jsonToggle(K key, String path) {
        nonNull(key, "key");
        nonNull(path, "path");
        RedisCommand cmd = RedisCommand.of(Command.JSON_TOGGLE)
                .put(marshaller.encode(key))
                .put(path);
        return execute(cmd);
    }

    Uni<Response> _jsonStrLen(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.JSON_STRLEN)
                .put(marshaller.encode(key));
        if (path != null) {
            cmd.put(path);
        }

        return execute(cmd);
    }

    Uni<Response> _jsonType(K key, String path) {
        nonNull(key, "key");
        RedisCommand cmd = RedisCommand.of(Command.JSON_TYPE)
                .put(marshaller.encode(key));
        if (path != null) {
            cmd.put(path);
        }

        return execute(cmd);
    }

}
