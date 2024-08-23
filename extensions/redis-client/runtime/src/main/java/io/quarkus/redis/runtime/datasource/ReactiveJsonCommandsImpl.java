package io.quarkus.redis.runtime.datasource;

import static io.smallrye.mutiny.helpers.ParameterValidation.nonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.redis.client.Response;
import io.vertx.redis.client.ResponseType;

public class ReactiveJsonCommandsImpl<K> extends AbstractJsonCommands<K> implements ReactiveJsonCommands<K> {

    private final ReactiveRedisDataSource reactive;

    public ReactiveJsonCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k) {
        super(redis, k);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value) {
        return _jsonSet(key, path, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json) {
        return _jsonSet(key, path, json)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        return _jsonSet(key, path, json, args)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json) {
        return _jsonSet(key, path, json)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        return _jsonSet(key, path, json, args)
                .replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value, JsonSetArgs args) {
        return _jsonSet(key, path, value, args)
                .replaceWithVoid();
    }

    @Override
    public <T> Uni<T> jsonGet(K key, Class<T> clazz) {
        nonNull(clazz, "clazz");
        return _jsonGet(key)
                .map(r -> {
                    var m = getJsonObject(r);
                    if (m != null) {
                        return m.mapTo(clazz);
                    }
                    return null;
                });
    }

    @Override
    public Uni<JsonObject> jsonGetObject(K key) {
        return _jsonGet(key)
                .map(ReactiveJsonCommandsImpl::getJsonObject);
    }

    @Override
    public Uni<JsonArray> jsonGetArray(K key) {
        return _jsonGet(key)
                .map(ReactiveJsonCommandsImpl::getJsonArray);
    }

    static JsonArray getJsonArray(Response r) {
        if (r == null || r.toString().equalsIgnoreCase("null")) { // JSON null
            return null;
        }
        // With Redis 7.2 the response is a BULK (String) but using a nested array.
        Buffer buffer = r.toBuffer();
        JsonArray array = buffer.toJsonArray();
        if (array.size() == 1 && array.getString(0).startsWith("[")) {
            return array.getJsonArray(0);
        }
        return array;
    }

    static JsonObject getJsonObject(Response r) {
        if (r == null || r.toString().equalsIgnoreCase("null")) { // JSON null
            return null;
        }
        // With Redis 7.2 the response is a BULK (String) but using a nested array.
        Buffer buffer = r.toBuffer();
        if (buffer.toJsonValue() instanceof JsonArray) {
            var array = buffer.toJsonArray();
            if (array.size() == 0) {
                return null;
            }
            return array.getJsonObject(0);
        }
        return r.toBuffer().toJsonObject();
    }

    static JsonArray getJsonArrayFromJsonGet(Response r) {
        if (r == null || r.toString().equalsIgnoreCase("null")) { // JSON null
            return null;
        }
        // With Redis 7.2 the response is a BULK (String) but using a nested array.
        Buffer buffer = r.toBuffer();
        if (buffer.toJsonValue() instanceof JsonArray) {
            var array = buffer.toJsonArray();
            if (array.size() == 0) {
                return new JsonArray();
            }
            return array;
        }
        return buffer.toJsonArray();
    }

    @Override
    public Uni<JsonArray> jsonGet(K key, String path) {
        return _jsonGet(key, path)
                .map(ReactiveJsonCommandsImpl::getJsonArrayFromJsonGet);
    }

    @Override
    public Uni<JsonObject> jsonGet(K key, String... paths) {
        return _jsonGet(key, paths)
                .map(ReactiveJsonCommandsImpl::getJsonObject);
    }

    @Override
    public <T> Uni<List<Integer>> jsonArrAppend(K key, String path, T... values) {
        return _jsonArrAppend(key, path, values)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public <T> Uni<List<Integer>> jsonArrIndex(K key, String path, T value, int start, int end) {
        return _jsonArrIndex(key, path, value, start, end)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    static List<Integer> decodeAsListOfInteger(Response r) {
        List<Integer> list = new ArrayList<>();
        if (r.type() == ResponseType.MULTI) {
            for (Response response : r) {
                list.add(response == null ? null : response.toInteger());
            }
        } else {
            list.add(r.toInteger());
        }
        return list;
    }

    @Override
    public <T> Uni<List<Integer>> jsonArrInsert(K key, String path, int index, T... values) {
        return _jsonArrInsert(key, path, index, values)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public Uni<List<Integer>> jsonArrLen(K key, String path) {
        return _jsonArrLen(key, path)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public <T> Uni<List<T>> jsonArrPop(K key, Class<T> clazz, String path, int index) {
        nonNull(clazz, "clazz");
        return _jsonArrPop(key, path, index)
                .map(r -> decodeArrPopResponse(clazz, r));
    }

    static <T> List<T> decodeArrPopResponse(Class<T> clazz, Response r) {
        List<T> list = new ArrayList<>();
        if (r == null) {
            list.add(null);
            return list;
        }
        if (r.type() == ResponseType.MULTI) {
            for (Response response : r) {
                list.add(response == null ? null : Json.decodeValue(response.toString(), clazz));
            }
        } else {
            list.add(Json.decodeValue(r.toString(), clazz));
        }
        return list;
    }

    @Override
    public Uni<List<Integer>> jsonArrTrim(K key, String path, int start, int stop) {
        return _jsonArrTrim(key, path, start, stop)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public Uni<Integer> jsonClear(K key, String path) {
        return _jsonClear(key, path)
                .map(Response::toInteger);
    }

    @Override
    public Uni<Integer> jsonDel(K key, String path) {
        return _jsonDel(key, path)
                .map(Response::toInteger);
    }

    @Override
    public Uni<List<JsonArray>> jsonMget(String path, K... keys) {
        return _jsonMget(path, keys)
                .map(ReactiveJsonCommandsImpl::decodeMGetResponse);
    }

    static List<JsonArray> decodeMGetResponse(Response r) {
        List<JsonArray> list = new ArrayList<>();
        if (r.type() == ResponseType.MULTI) {
            for (Response response : r) {
                list.add(response == null ? null : response.toBuffer().toJsonArray());
            }
        } else {
            list.add(r.toBuffer().toJsonArray());
        }
        return list;
    }

    @Override
    public Uni<Void> jsonNumincrby(K key, String path, double value) {
        return _jsonNumincrby(key, path, value)
                .replaceWithVoid();
    }

    @Override
    public Uni<List<List<String>>> jsonObjKeys(K key, String path) {
        return _jsonObjKeys(key, path)
                .map(ReactiveJsonCommandsImpl::decodeObjKeysResponse);
    }

    static List<List<String>> decodeObjKeysResponse(Response r) {
        List<List<String>> list = new ArrayList<>();
        if (r.type() == ResponseType.MULTI) {
            List<String> sub = new ArrayList<>();
            for (Response item : r) {
                if (item == null) {
                    list.add(null);
                } else {
                    if (item.type() == ResponseType.MULTI) {
                        for (Response nested : item) {
                            sub.add(nested == null ? null : nested.toString());
                        }
                    } else { // BULK
                        sub.add(item.toString());
                    }
                }
            }
            list.add(sub);
        } else {
            list.add(null);
        }
        return list;
    }

    @Override
    public Uni<List<Integer>> jsonObjLen(K key, String path) {
        return _jsonObjLen(key, path)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public Uni<List<Integer>> jsonStrAppend(K key, String path, String value) {
        return _jsonStrAppend(key, path, value)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public Uni<List<Integer>> jsonStrLen(K key, String path) {
        return _jsonStrLen(key, path)
                .map(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
    }

    @Override
    public Uni<List<Boolean>> jsonToggle(K key, String path) {
        return _jsonToggle(key, path)
                .map(ReactiveJsonCommandsImpl::decodeToggleResponse);
    }

    static List<Boolean> decodeToggleResponse(Response r) {
        List<Boolean> list = new ArrayList<>();
        if (r.type() == ResponseType.MULTI) {
            for (Response response : r) {
                list.add(response == null ? null : response.toBoolean());
            }
        } else {
            list.add(r.toBoolean());
        }
        return list;
    }

    @Override
    public Uni<List<String>> jsonType(K key, String path) {
        return _jsonType(key, path)
                .map(ReactiveJsonCommandsImpl::decodeTypeResponse);
    }

    static List<String> decodeTypeResponse(Response r) {
        List<String> list = new ArrayList<>();
        if (r.type() == ResponseType.MULTI) {
            for (Response response : r) {
                if (response == null) {
                    list.add(null);
                } else if (response.type() == ResponseType.MULTI) {
                    // Redis 7.2 behavior
                    for (Response nested : response) {
                        list.add(nested == null ? null : nested.toString());
                    }
                } else {
                    list.add(response.toString());
                }
            }
        } else {
            list.add(r.toString());
        }
        return list;
    }
}
