package io.quarkus.redis.lettuce.runtime.internal.json;

import static io.quarkus.redis.runtime.datasource.Validation.notNullOrBlank;
import static io.smallrye.mutiny.helpers.ParameterValidation.doesNotContainNull;
import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.lettuce.core.json.JsonPath;
import io.lettuce.core.json.JsonType;
import io.lettuce.core.json.arguments.JsonRangeArgs;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.quarkus.redis.lettuce.runtime.internal.AbstractLettuceCommands;
import io.quarkus.redis.lettuce.runtime.internal.LettuceCommand;
import io.quarkus.redis.lettuce.runtime.internal.LettuceConnection;
import io.quarkus.redis.runtime.datasource.Marshaller;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Lettuce-backed implementation of {@link ReactiveJsonCommands}.
 *
 * @param <K> the key type
 */
public class LettuceReactiveJsonCommandsImpl<K> extends AbstractLettuceCommands<K, K>
        implements ReactiveJsonCommands<K> {

    private static final JsonSetArgs JSON_SET_DEFAULT = new JsonSetArgs();

    private final ReactiveRedisDataSource dataSource;

    public LettuceReactiveJsonCommandsImpl(ReactiveRedisDataSource dataSource,
            LettuceConnection connection, Type keyType) {
        super(connection, keyType, keyType, new Marshaller(keyType));
        this.dataSource = dataSource;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value) {
        return _jsonSet(key, path, value).toUni();
    }

    <T> LettuceCommand<String, Void> _jsonSet(K key, String path, T value) {
        return _jsonSet(key, path, Json.encode(value), JSON_SET_DEFAULT);
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json) {
        return _jsonSet(key, path, json).toUni();
    }

    LettuceCommand<String, Void> _jsonSet(K key, String path, JsonObject json) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.encode(), JSON_SET_DEFAULT);
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        return _jsonSet(key, path, json, args).toUni();
    }

    LettuceCommand<String, Void> _jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.encode(), args);
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json) {
        return _jsonSet(key, path, json).toUni();
    }

    LettuceCommand<String, Void> _jsonSet(K key, String path, JsonArray json) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.encode(), JSON_SET_DEFAULT);
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        return _jsonSet(key, path, json, args).toUni();
    }

    LettuceCommand<String, Void> _jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        nonNull(json, "json");
        return _jsonSet(key, path, json.encode(), args);
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value, JsonSetArgs args) {
        return _jsonSet(key, path, value, args).toUni();
    }

    <T> LettuceCommand<String, Void> _jsonSet(K key, String path, T value, JsonSetArgs args) {
        return _jsonSet(key, path, Json.encode(value), args);
    }

    private LettuceCommand<String, Void> _jsonSet(K key, String path, String json, JsonSetArgs args) {
        nonNull(key, "key");
        notNullOrBlank(path, "path");
        nonNull(args, "args");
        io.lettuce.core.json.arguments.JsonSetArgs lettuceArgs = LettuceJsonCommandsConverter.toLettuceJsonSetArgs(args);
        return LettuceCommand.discarding(() -> async.jsonSet(marshaller.encode(key), JsonPath.of(path), json, lettuceArgs));
    }

    @Override
    public <T> Uni<T> jsonGet(K key, Class<T> clazz) {
        return _jsonGet(key, clazz).toUni();
    }

    <T> LettuceCommand<List<String>, T> _jsonGet(K key, Class<T> clazz) {
        nonNull(key, "key");
        nonNull(clazz, "clazz");
        return LettuceCommand.of(() -> async.jsonGetRaw(marshaller.encode(key), JsonPath.ROOT_PATH), raw -> {
            JsonObject object = decodeRootObject(first(raw));
            return object == null ? null : object.mapTo(clazz);
        });
    }

    @Override
    public Uni<JsonObject> jsonGetObject(K key) {
        return _jsonGetObject(key).toUni();
    }

    LettuceCommand<List<String>, JsonObject> _jsonGetObject(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.jsonGetRaw(marshaller.encode(key), JsonPath.ROOT_PATH),
                raw -> decodeRootObject(first(raw)));
    }

    @Override
    public Uni<JsonArray> jsonGetArray(K key) {
        return _jsonGetArray(key).toUni();
    }

    LettuceCommand<List<String>, JsonArray> _jsonGetArray(K key) {
        nonNull(key, "key");
        return LettuceCommand.of(() -> async.jsonGetRaw(marshaller.encode(key), JsonPath.ROOT_PATH),
                raw -> decodeRootArray(first(raw)));
    }

    @Override
    public Uni<JsonArray> jsonGet(K key, String path) {
        return _jsonGet(key, path).toUni();
    }

    LettuceCommand<List<String>, JsonArray> _jsonGet(K key, String path) {
        nonNull(key, "key");
        nonNull(path, "path");
        return LettuceCommand.of(() -> async.jsonGetRaw(marshaller.encode(key), JsonPath.of(path)),
                raw -> decodeJsonArrayFromJsonGet(first(raw)));
    }

    @Override
    public Uni<JsonObject> jsonGet(K key, String... paths) {
        return _jsonGet(key, paths).toUni();
    }

    LettuceCommand<List<String>, JsonObject> _jsonGet(K key, String... paths) {
        nonNull(key, "key");
        doesNotContainNull(paths, "paths");
        JsonPath[] jsonPaths = new JsonPath[paths.length];
        for (int i = 0; i < paths.length; i++) {
            jsonPaths[i] = JsonPath.of(paths[i]);
        }
        return LettuceCommand.of(() -> async.jsonGetRaw(marshaller.encode(key), jsonPaths),
                raw -> decodeJsonObject(first(raw)));
    }

    @SafeVarargs
    @Override
    public final <T> Uni<List<Integer>> jsonArrAppend(K key, String path, T... values) {
        return _jsonArrAppend(key, path, values).toUni();
    }

    @SafeVarargs
    final <T> LettuceCommand<List<Long>, List<Integer>> _jsonArrAppend(K key, String path, T... values) {
        nonNull(key, "key");
        doesNotContainNull(values, "values");
        String[] encoded = encodeJson(values);
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonArrappend(marshaller.encode(key), encoded),
                    AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonArrappend(marshaller.encode(key), JsonPath.of(path), encoded),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public <T> Uni<List<Integer>> jsonArrIndex(K key, String path, T value, int start, int end) {
        return _jsonArrIndex(key, path, value, start, end).toUni();
    }

    <T> LettuceCommand<List<Long>, List<Integer>> _jsonArrIndex(K key, String path, T value, int start, int end) {
        nonNull(key, "key");
        nonNull(path, "path");
        nonNull(value, "value");
        String encoded = Json.encode(value);
        JsonRangeArgs range = LettuceJsonCommandsConverter.toLettuceJsonRangeArgs(start, end);
        return LettuceCommand.of(() -> async.jsonArrindex(marshaller.encode(key), JsonPath.of(path), encoded, range),
                AbstractLettuceCommands::toInteger);
    }

    @SafeVarargs
    @Override
    public final <T> Uni<List<Integer>> jsonArrInsert(K key, String path, int index, T... values) {
        return _jsonArrInsert(key, path, index, values).toUni();
    }

    @SafeVarargs
    final <T> LettuceCommand<List<Long>, List<Integer>> _jsonArrInsert(K key, String path, int index, T... values) {
        nonNull(key, "key");
        nonNull(path, "path");
        doesNotContainNull(values, "values");
        String[] encoded = encodeJson(values);
        return LettuceCommand.of(() -> async.jsonArrinsert(marshaller.encode(key), JsonPath.of(path), index, encoded),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<List<Integer>> jsonArrLen(K key, String path) {
        return _jsonArrLen(key, path).toUni();
    }

    LettuceCommand<List<Long>, List<Integer>> _jsonArrLen(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonArrlen(marshaller.encode(key)), AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonArrlen(marshaller.encode(key), JsonPath.of(path)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public <T> Uni<List<T>> jsonArrPop(K key, Class<T> clazz, String path, int index) {
        return _jsonArrPop(key, clazz, path, index).toUni();
    }

    <T> LettuceCommand<List<String>, List<T>> _jsonArrPop(K key, Class<T> clazz, String path, int index) {
        nonNull(key, "key");
        nonNull(clazz, "clazz");
        if (path == null && index == -1) {
            return LettuceCommand.of(() -> async.jsonArrpopRaw(marshaller.encode(key)), raw -> decodeJsonValues(raw, clazz));
        }
        JsonPath jsonPath = path == null ? JsonPath.ROOT_PATH : JsonPath.of(path);
        return LettuceCommand.of(() -> async.jsonArrpopRaw(marshaller.encode(key), jsonPath, index),
                raw -> decodeJsonValues(raw, clazz));
    }

    @Override
    public Uni<List<Integer>> jsonArrTrim(K key, String path, int start, int stop) {
        return _jsonArrTrim(key, path, start, stop).toUni();
    }

    LettuceCommand<List<Long>, List<Integer>> _jsonArrTrim(K key, String path, int start, int stop) {
        nonNull(key, "key");
        nonNull(path, "path");
        JsonRangeArgs range = LettuceJsonCommandsConverter.toLettuceJsonRangeArgs(start, stop);
        return LettuceCommand.of(() -> async.jsonArrtrim(marshaller.encode(key), JsonPath.of(path), range),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Integer> jsonClear(K key, String path) {
        return _jsonClear(key, path).toUni();
    }

    LettuceCommand<Long, Integer> _jsonClear(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonClear(marshaller.encode(key)), AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonClear(marshaller.encode(key), JsonPath.of(path)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<Integer> jsonDel(K key, String path) {
        return _jsonDel(key, path).toUni();
    }

    LettuceCommand<Long, Integer> _jsonDel(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonDel(marshaller.encode(key)), AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonDel(marshaller.encode(key), JsonPath.of(path)),
                AbstractLettuceCommands::toInteger);
    }

    @SafeVarargs
    @Override
    public final Uni<List<JsonArray>> jsonMget(String path, K... keys) {
        return _jsonMget(path, keys).toUni();
    }

    @SafeVarargs
    final LettuceCommand<List<String>, List<JsonArray>> _jsonMget(String path, K... keys) {
        notNullOrBlank(path, "path");
        doesNotContainNull(keys, "keys");
        byte[][] encodedKeys = marshaller.encodeAsArray(keys);
        return LettuceCommand.of(() -> async.jsonMGetRaw(JsonPath.of(path), encodedKeys),
                LettuceReactiveJsonCommandsImpl::decodeJsonArrays);
    }

    @Override
    public Uni<Void> jsonNumincrby(K key, String path, double value) {
        return _jsonNumincrby(key, path, value).toUni();
    }

    LettuceCommand<List<Number>, Void> _jsonNumincrby(K key, String path, double value) {
        nonNull(key, "key");
        notNullOrBlank(path, "path");
        return LettuceCommand.discarding(() -> async.jsonNumincrby(marshaller.encode(key), JsonPath.of(path), value));
    }

    @Override
    public Uni<List<List<String>>> jsonObjKeys(K key, String path) {
        return _jsonObjKeys(key, path).toUni();
    }

    LettuceCommand<List<byte[]>, List<List<String>>> _jsonObjKeys(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonObjkeys(marshaller.encode(key)),
                    LettuceReactiveJsonCommandsImpl::decodeObjKeys);
        }
        return LettuceCommand.of(() -> async.jsonObjkeys(marshaller.encode(key), JsonPath.of(path)),
                LettuceReactiveJsonCommandsImpl::decodeObjKeys);
    }

    @Override
    public Uni<List<Integer>> jsonObjLen(K key, String path) {
        return _jsonObjLen(key, path).toUni();
    }

    LettuceCommand<List<Long>, List<Integer>> _jsonObjLen(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonObjlen(marshaller.encode(key)), AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonObjlen(marshaller.encode(key), JsonPath.of(path)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<List<Integer>> jsonStrAppend(K key, String path, String value) {
        return _jsonStrAppend(key, path, value).toUni();
    }

    LettuceCommand<List<Long>, List<Integer>> _jsonStrAppend(K key, String path, String value) {
        nonNull(key, "key");
        nonNull(value, "value");
        String encoded = Json.encode(value);
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonStrappend(marshaller.encode(key), encoded),
                    AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonStrappend(marshaller.encode(key), JsonPath.of(path), encoded),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<List<Integer>> jsonStrLen(K key, String path) {
        return _jsonStrLen(key, path).toUni();
    }

    LettuceCommand<List<Long>, List<Integer>> _jsonStrLen(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonStrlen(marshaller.encode(key)), AbstractLettuceCommands::toInteger);
        }
        return LettuceCommand.of(() -> async.jsonStrlen(marshaller.encode(key), JsonPath.of(path)),
                AbstractLettuceCommands::toInteger);
    }

    @Override
    public Uni<List<Boolean>> jsonToggle(K key, String path) {
        return _jsonToggle(key, path).toUni();
    }

    LettuceCommand<List<Long>, List<Boolean>> _jsonToggle(K key, String path) {
        nonNull(key, "key");
        nonNull(path, "path");
        return LettuceCommand.of(() -> async.jsonToggle(marshaller.encode(key), JsonPath.of(path)),
                LettuceReactiveJsonCommandsImpl::decodeToggle);
    }

    @Override
    public Uni<List<String>> jsonType(K key, String path) {
        return _jsonType(key, path).toUni();
    }

    LettuceCommand<List<JsonType>, List<String>> _jsonType(K key, String path) {
        nonNull(key, "key");
        if (path == null) {
            return LettuceCommand.of(() -> async.jsonType(marshaller.encode(key)),
                    LettuceReactiveJsonCommandsImpl::decodeJsonTypes);
        }
        return LettuceCommand.of(() -> async.jsonType(marshaller.encode(key), JsonPath.of(path)),
                LettuceReactiveJsonCommandsImpl::decodeJsonTypes);
    }

    private static <T> String[] encodeJson(T[] values) {
        String[] encoded = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            encoded[i] = Json.encode(values[i]);
        }
        return encoded;
    }

    private static String first(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return null;
        }
        return raw.get(0);
    }

    private static boolean isJsonNull(String raw) {
        return raw == null || raw.equalsIgnoreCase("null");
    }

    static JsonObject decodeJsonObject(String raw) {
        if (isJsonNull(raw)) {
            return null;
        }
        return new JsonObject(raw);
    }

    private static JsonArray rootMatches(String raw) {
        if (isJsonNull(raw)) {
            return null;
        }
        JsonArray matches = new JsonArray(raw);
        if (matches.isEmpty() || matches.getValue(0) == null) {
            return null;
        }
        return matches;
    }

    static JsonObject decodeRootObject(String raw) {
        JsonArray matches = rootMatches(raw);
        return matches == null ? null : matches.getJsonObject(0);
    }

    static JsonArray decodeRootArray(String raw) {
        JsonArray matches = rootMatches(raw);
        return matches == null ? null : matches.getJsonArray(0);
    }

    static JsonArray decodeJsonArrayFromJsonGet(String raw) {
        if (isJsonNull(raw)) {
            return null;
        }
        return new JsonArray(raw);
    }

    static List<JsonArray> decodeJsonArrays(List<String> raw) {
        List<JsonArray> list = new ArrayList<>();
        for (String item : orEmpty(raw)) {
            list.add(item == null ? null : new JsonArray(item));
        }
        return list;
    }

    static <T> List<T> decodeJsonValues(List<String> raw, Class<T> clazz) {
        List<T> list = new ArrayList<>();
        for (String item : orEmpty(raw)) {
            list.add(item == null ? null : Json.decodeValue(item, clazz));
        }
        return list;
    }

    static List<List<String>> decodeObjKeys(List<byte[]> raw) {
        List<List<String>> list = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (byte[] item : orEmpty(raw)) {
            if (item == null) {
                list.add(null);
            } else {
                keys.add(new String(item, StandardCharsets.UTF_8));
            }
        }
        list.add(keys);
        return list;
    }

    static List<Boolean> decodeToggle(List<Long> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<Boolean> list = new ArrayList<>(raw.size());
        for (Long item : raw) {
            list.add(item == null ? null : item == 1L);
        }
        return list;
    }

    static String decodeJsonType(JsonType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case OBJECT -> "object";
            case ARRAY -> "array";
            case STRING -> "string";
            case INTEGER -> "integer";
            case NUMBER -> "number";
            case BOOLEAN -> "boolean";
            case UNKNOWN -> "null";
        };
    }

    static List<String> decodeJsonTypes(List<JsonType> raw) {
        if (raw == null || raw.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> list = new ArrayList<>(raw.size());
        for (JsonType type : raw) {
            list.add(decodeJsonType(type));
        }
        return list;
    }

}
