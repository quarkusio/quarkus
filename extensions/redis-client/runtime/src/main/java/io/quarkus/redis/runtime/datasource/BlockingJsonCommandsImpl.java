package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.json.JsonCommands;
import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveJsonCommands;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BlockingJsonCommandsImpl<K> extends AbstractRedisCommandGroup implements JsonCommands<K> {

    private final ReactiveJsonCommands<K> reactive;

    public BlockingJsonCommandsImpl(RedisDataSource ds, ReactiveJsonCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public <T> void jsonSet(K key, String path, T value) {
        reactive.jsonSet(key, path, value).await().atMost(timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonObject json) {
        reactive.jsonSet(key, path, json).await().atMost(timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        reactive.jsonSet(key, path, json, args).await().atMost(timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonArray json) {
        reactive.jsonSet(key, path, json).await().atMost(timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        reactive.jsonSet(key, path, json, args).await().atMost(timeout);
    }

    @Override
    public <T> void jsonSet(K key, String path, T value, JsonSetArgs args) {
        reactive.jsonSet(key, path, value, args).await().atMost(timeout);
    }

    @Override
    public <T> T jsonGet(K key, Class<T> clazz) {
        return reactive.jsonGet(key, clazz).await().atMost(timeout);
    }

    @Override
    public JsonObject jsonGetObject(K key) {
        return reactive.jsonGetObject(key).await().atMost(timeout);
    }

    @Override
    public JsonArray jsonGetArray(K key) {
        return reactive.jsonGetArray(key).await().atMost(timeout);
    }

    @Override
    public JsonArray jsonGet(K key, String path) {
        return reactive.jsonGet(key, path).await().atMost(timeout);
    }

    @Override
    public JsonObject jsonGet(K key, String... paths) {
        return reactive.jsonGet(key, paths).await().atMost(timeout);
    }

    @Override
    public <T> List<Integer> jsonArrAppend(K key, String path, T... values) {
        return reactive.jsonArrAppend(key, path, values).await().atMost(timeout);
    }

    @Override
    public <T> List<Integer> jsonArrIndex(K key, String path, T value, int start, int end) {
        return reactive.jsonArrIndex(key, path, value, start, end).await().atMost(timeout);
    }

    @Override
    public <T> List<Integer> jsonArrIndex(K key, String path, T value) {
        return reactive.jsonArrIndex(key, path, value).await().atMost(timeout);
    }

    @Override
    public <T> List<Integer> jsonArrInsert(K key, String path, int index, T... values) {
        return reactive.jsonArrInsert(key, path, index, values).await().atMost(timeout);
    }

    @Override
    public List<Integer> jsonArrLen(K key, String path) {
        return reactive.jsonArrLen(key, path).await().atMost(timeout);
    }

    @Override
    public <T> List<T> jsonArrPop(K key, Class<T> clazz, String path, int index) {
        return reactive.jsonArrPop(key, clazz, path, index).await().atMost(timeout);
    }

    @Override
    public List<Integer> jsonArrTrim(K key, String path, int start, int stop) {
        return reactive.jsonArrTrim(key, path, start, stop).await().atMost(timeout);
    }

    @Override
    public int jsonClear(K key, String path) {
        return reactive.jsonClear(key, path).await().atMost(timeout);
    }

    @Override
    public int jsonDel(K key, String path) {
        return reactive.jsonDel(key, path).await().atMost(timeout);
    }

    @Override
    public List<JsonArray> jsonMget(String path, K... keys) {
        return reactive.jsonMget(path, keys).await().atMost(timeout);
    }

    @Override
    public void jsonNumincrby(K key, String path, double value) {
        reactive.jsonNumincrby(key, path, value).await().atMost(timeout);
    }

    @Override
    public List<List<String>> jsonObjKeys(K key, String path) {
        return reactive.jsonObjKeys(key, path).await().atMost(timeout);
    }

    @Override
    public List<Integer> jsonObjLen(K key, String path) {
        return reactive.jsonObjLen(key, path).await().atMost(timeout);
    }

    @Override
    public List<Integer> jsonStrAppend(K key, String path, String value) {
        return reactive.jsonStrAppend(key, path, value).await().atMost(timeout);
    }

    @Override
    public List<Integer> jsonStrLen(K key, String path) {
        return reactive.jsonStrLen(key, path).await().atMost(timeout);
    }

    @Override
    public List<Boolean> jsonToggle(K key, String path) {
        return reactive.jsonToggle(key, path).await().atMost(timeout);
    }

    @Override
    public List<String> jsonType(K key, String path) {
        return reactive.jsonType(key, path).await().atMost(timeout);
    }
}
