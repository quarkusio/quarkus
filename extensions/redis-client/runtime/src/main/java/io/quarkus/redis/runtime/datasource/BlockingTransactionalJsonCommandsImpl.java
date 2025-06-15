package io.quarkus.redis.runtime.datasource;

import java.time.Duration;

import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.json.TransactionalJsonCommands;
import io.quarkus.redis.datasource.transactions.TransactionalRedisDataSource;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class BlockingTransactionalJsonCommandsImpl<K> extends AbstractTransactionalRedisCommandGroup
        implements TransactionalJsonCommands<K> {

    private final ReactiveTransactionalJsonCommands<K> reactive;

    public BlockingTransactionalJsonCommandsImpl(TransactionalRedisDataSource ds,
            ReactiveTransactionalJsonCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public <T> void jsonSet(K key, String path, T value) {
        this.reactive.jsonSet(key, path, value).await().atMost(this.timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonObject json) {
        this.reactive.jsonSet(key, path, json).await().atMost(this.timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        this.reactive.jsonSet(key, path, json, args).await().atMost(this.timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonArray json) {
        this.reactive.jsonSet(key, path, json).await().atMost(this.timeout);
    }

    @Override
    public void jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        this.reactive.jsonSet(key, path, json, args).await().atMost(this.timeout);
    }

    @Override
    public <T> void jsonSet(K key, String path, T value, JsonSetArgs args) {
        this.reactive.jsonSet(key, path, value, args).await().atMost(this.timeout);
    }

    @Override
    public <T> void jsonGet(K key, Class<T> clazz) {
        this.reactive.jsonGet(key, clazz).await().atMost(this.timeout);
    }

    @Override
    public void jsonGetObject(K key) {
        this.reactive.jsonGetObject(key).await().atMost(this.timeout);
    }

    @Override
    public void jsonGetArray(K key) {
        this.reactive.jsonGetArray(key).await().atMost(this.timeout);
    }

    @Override
    public void jsonGet(K key, String path) {
        this.reactive.jsonGet(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonGet(K key, String... paths) {
        this.reactive.jsonGet(key, paths).await().atMost(this.timeout);
    }

    @Override
    public <T> void jsonArrAppend(K key, String path, T... values) {
        this.reactive.jsonArrAppend(key, path, values).await().atMost(this.timeout);
    }

    @Override
    public <T> void jsonArrIndex(K key, String path, T value, int start, int end) {
        this.reactive.jsonArrIndex(key, path, value, start, end).await().atMost(this.timeout);
    }

    @Override
    public <T> void jsonArrInsert(K key, String path, int index, T... values) {
        this.reactive.jsonArrInsert(key, path, index, values).await().atMost(this.timeout);
    }

    @Override
    public void jsonArrLen(K key, String path) {
        this.reactive.jsonArrLen(key, path).await().atMost(this.timeout);
    }

    @Override
    public <T> void jsonArrPop(K key, Class<T> clazz, String path, int index) {
        this.reactive.jsonArrPop(key, clazz, path, index).await().atMost(this.timeout);
    }

    @Override
    public void jsonArrTrim(K key, String path, int start, int stop) {
        this.reactive.jsonArrTrim(key, path, stop, stop).await().atMost(this.timeout);
    }

    @Override
    public void jsonClear(K key, String path) {
        this.reactive.jsonClear(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonDel(K key, String path) {
        this.reactive.jsonDel(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonMget(String path, K... keys) {
        this.reactive.jsonMget(path, keys).await().atMost(this.timeout);
    }

    @Override
    public void jsonNumincrby(K key, String path, double value) {
        this.reactive.jsonNumincrby(key, path, value).await().atMost(this.timeout);
    }

    @Override
    public void jsonObjKeys(K key, String path) {
        this.reactive.jsonObjKeys(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonObjLen(K key, String path) {
        this.reactive.jsonObjLen(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonStrAppend(K key, String path, String value) {
        this.reactive.jsonStrAppend(key, path, value).await().atMost(this.timeout);
    }

    @Override
    public void jsonStrLen(K key, String path) {
        this.reactive.jsonStrLen(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonToggle(K key, String path) {
        this.reactive.jsonToggle(key, path).await().atMost(this.timeout);
    }

    @Override
    public void jsonType(K key, String path) {
        this.reactive.jsonType(key, path).await().atMost(this.timeout);
    }
}
