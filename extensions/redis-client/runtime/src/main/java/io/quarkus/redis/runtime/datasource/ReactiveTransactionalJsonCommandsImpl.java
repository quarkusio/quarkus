package io.quarkus.redis.runtime.datasource;

import static io.quarkus.redis.runtime.datasource.ReactiveJsonCommandsImpl.decodeArrPopResponse;
import static io.quarkus.redis.runtime.datasource.ReactiveJsonCommandsImpl.getJsonObject;

import io.quarkus.redis.datasource.json.JsonSetArgs;
import io.quarkus.redis.datasource.json.ReactiveTransactionalJsonCommands;
import io.quarkus.redis.datasource.transactions.ReactiveTransactionalRedisDataSource;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTransactionalJsonCommandsImpl<K> extends AbstractTransactionalCommands
        implements ReactiveTransactionalJsonCommands<K> {

    private final ReactiveJsonCommandsImpl<K> reactive;

    public ReactiveTransactionalJsonCommandsImpl(ReactiveTransactionalRedisDataSource ds,
            ReactiveJsonCommandsImpl<K> reactive, TransactionHolder tx) {
        super(ds, tx);
        this.reactive = reactive;
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value) {
        this.tx.enqueue(resp -> null);
        return this.reactive._jsonSet(key, path, value).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json) {
        this.tx.enqueue(resp -> null);
        return this.reactive._jsonSet(key, path, json).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonObject json, JsonSetArgs args) {
        this.tx.enqueue(resp -> null);
        return this.reactive._jsonSet(key, path, json, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json) {
        this.tx.enqueue(resp -> null);
        return this.reactive._jsonSet(key, path, json).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonSet(K key, String path, JsonArray json, JsonSetArgs args) {
        this.tx.enqueue(resp -> null);
        return this.reactive._jsonSet(key, path, json, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonSet(K key, String path, T value, JsonSetArgs args) {
        this.tx.enqueue(resp -> null);
        return this.reactive._jsonSet(key, path, value, args).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonGet(K key, Class<T> clazz) {
        this.tx.enqueue(r -> {
            var m = getJsonObject(r);
            if (m != null) {
                return m.mapTo(clazz);
            }
            return null;
        });
        return this.reactive._jsonGet(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonGetObject(K key) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::getJsonObject);
        return this.reactive._jsonGet(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonGetArray(K key) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::getJsonArray);
        return this.reactive._jsonGet(key).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonGet(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::getJsonArrayFromJsonGet);
        return this.reactive._jsonGet(key, path).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonGet(K key, String... paths) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::getJsonObject);
        return this.reactive._jsonGet(key, paths).invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonArrAppend(K key, String path, T... values) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonArrAppend(key, path, values)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonArrIndex(K key, String path, T value, int start, int end) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonArrIndex(key, path, value, start, end)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonArrInsert(K key, String path, int index, T... values) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonArrInsert(key, path, index, values)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonArrLen(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonArrLen(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public <T> Uni<Void> jsonArrPop(K key, Class<T> clazz, String path, int index) {
        this.tx.enqueue(r -> decodeArrPopResponse(clazz, r));
        return this.reactive._jsonArrPop(key, path, index)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonArrTrim(K key, String path, int start, int stop) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonArrTrim(key, path, start, stop)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonClear(K key, String path) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._jsonClear(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonDel(K key, String path) {
        this.tx.enqueue(Response::toInteger);
        return this.reactive._jsonDel(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonMget(String path, K... keys) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeMGetResponse);
        return this.reactive._jsonMget(path, keys)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonNumincrby(K key, String path, double value) {
        this.tx.enqueue(r -> null);
        return this.reactive._jsonNumincrby(key, path, value)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonObjKeys(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeObjKeysResponse);
        return this.reactive._jsonObjKeys(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonObjLen(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonObjLen(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonStrAppend(K key, String path, String value) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonStrAppend(key, path, value)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonStrLen(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeAsListOfInteger);
        return this.reactive._jsonStrLen(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonToggle(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeToggleResponse);
        return this.reactive._jsonToggle(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

    @Override
    public Uni<Void> jsonType(K key, String path) {
        this.tx.enqueue(ReactiveJsonCommandsImpl::decodeTypeResponse);
        return this.reactive._jsonType(key, path)
                .invoke(this::queuedOrDiscard).replaceWithVoid();
    }

}
