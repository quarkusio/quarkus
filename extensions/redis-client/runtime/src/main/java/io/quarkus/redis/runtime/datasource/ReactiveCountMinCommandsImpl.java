package io.quarkus.redis.runtime.datasource;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.ReactiveRedisCommands;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.countmin.ReactiveCountMinCommands;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.redis.client.Response;

public class ReactiveCountMinCommandsImpl<K, V> extends AbstractCountMinCommands<K, V>
        implements ReactiveCountMinCommands<K, V>, ReactiveRedisCommands {

    private final ReactiveRedisDataSource reactive;

    public ReactiveCountMinCommandsImpl(ReactiveRedisDataSourceImpl redis, Type k, Type v) {
        super(redis, k, v);
        this.reactive = redis;
    }

    @Override
    public ReactiveRedisDataSource getDataSource() {
        return reactive;
    }

    @Override
    public Uni<Long> cmsIncrBy(K key, V value, long increment) {
        return super._cmsIncrBy(key, value, increment)
                .map(r -> r.get(0).toLong());
    }

    @Override
    public Uni<Map<V, Long>> cmsIncrBy(K key, Map<V, Long> couples) {
        return super._cmsIncrBy(key, couples)
                .map(r -> decodeAsMapVL(couples, r));
    }

    Map<V, Long> decodeAsMapVL(Map<V, Long> couples, Response r) {
        Map<V, Long> result = new LinkedHashMap<>();
        var iterator = couples.keySet().iterator();
        for (Response response : r) {
            result.put(iterator.next(), response.toLong());
        }
        return result;
    }

    @Override
    public Uni<Void> cmsInitByDim(K key, long width, long depth) {
        return super._cmsInitByDim(key, width, depth)
                .replaceWithVoid();
    }

    @Override
    public Uni<Void> cmsInitByProb(K key, double error, double probability) {
        return super._cmsInitByProb(key, error, probability)
                .replaceWithVoid();
    }

    @Override
    public Uni<Long> cmsQuery(K key, V item) {
        return super._cmsQuery(key, item)
                .map(r -> decodeAListOfLongs(r).get(0));
    }

    @Override
    public Uni<List<Long>> cmsQuery(K key, V... items) {
        return super._cmsQuery(key, items)
                .map(this::decodeAListOfLongs);
    }

    List<Long> decodeAListOfLongs(Response r) {
        List<Long> results = new ArrayList<>();
        for (Response response : r) {
            results.add(response.toLong());
        }
        return results;
    }

    @Override
    public Uni<Void> cmsMerge(K dest, List<K> src, List<Integer> weight) {
        return super._cmsMerge(dest, src, weight)
                .replaceWithVoid();
    }
}
