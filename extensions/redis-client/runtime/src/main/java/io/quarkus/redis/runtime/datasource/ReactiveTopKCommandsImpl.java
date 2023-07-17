package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.topk.ReactiveTopKCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveTopKCommandsImpl<K, V> extends AbstractTopKCommands<K, V>
        implements ReactiveTopKCommands<K, V>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;
    final Type typeOfValue;

    public ReactiveTopKCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.typeOfValue = v;
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<V> topkAdd(K key, V item) {
        return super._topkAdd(key, item)
                .map(r -> marshaller.<V> decodeAsList(r, typeOfValue).get(0));
    }

    @Override
    public Uni<List<V>> topkAdd(K key, V... items) {
        return super._topkAdd(key, items)
                .map(r -> marshaller.decodeAsList(r, typeOfValue));
    }

    @Override
    public Uni<V> topkIncrBy(K key, V item, int increment) {
        return super._topkIncrBy(key, item, increment)
                .map(r -> marshaller.<V> decodeAsList(r, typeOfValue).get(0));
    }

    @Override
    public Uni<Map<V, V>> topkIncrBy(K key, Map<V, Integer> couples) {
        return super._topkIncrBy(key, couples)
                .map(r -> decodeAsMapVV(couples, r));
    }

    Map<V, V> decodeAsMapVV(Map<V, Integer> couples, Response r) {
        Map<V, V> map = new LinkedHashMap<>();
        Iterator<V> iterator = couples.keySet().iterator();
        for (Response response : r) {
            map.put(iterator.next(), marshaller.decode(typeOfValue, response));
        }
        return map;
    }

    @Override
    public Uni<List<V>> topkList(K key) {
        return super._topkList(key)
                .map(r -> marshaller.decodeAsList(r, typeOfValue));
    }

    @Override
    public Uni<Map<V, Integer>> topkListWithCount(K key) {
        return super._topkListWithCount(key)
                .map(this::decodeAsMapVInt);
    }

    Map<V, Integer> decodeAsMapVInt(Response r) {
        Map<V, Integer> map = new LinkedHashMap<>();
        V current = null;
        for (Response response : r) {
            if (current == null) {
                current = decodeV(response);
            } else {
                map.put(current, response.toInteger());
                current = null;
            }
        }
        return map;
    }

    @Override
    public Uni<Boolean> topkQuery(K key, V item) {
        return super._topkQuery(key, item)
                .map(r -> marshaller.decodeAsList(r, Response::toBoolean).get(0));
    }

    @Override
    public Uni<List<Boolean>> topkQuery(K key, V... items) {
        return super._topkQuery(key, items)
                .map(r -> marshaller.decodeAsList(r, Response::toBoolean));
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk) {
        return super._topkReserve(key, topk)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> topkReserve(K key, int topk, int width, int depth, double decay) {
        return super._topkReserve(key, topk, width, depth, decay)
                .replaceWithVoid();
    }

    V decodeV(Response r) {
        return marshaller.decode(typeOfValue, r);
    }
}
