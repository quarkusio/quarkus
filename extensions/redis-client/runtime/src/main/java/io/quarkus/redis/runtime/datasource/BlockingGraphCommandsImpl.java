package io.quarkus.redis.runtime.datasource;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.graph.GraphCommands;
import io.quarkus.redis.datasource.graph.GraphQueryResponseItem;
import io.quarkus.redis.datasource.graph.ReactiveGraphCommands;

public class BlockingGraphCommandsImpl<K> extends AbstractRedisCommandGroup implements GraphCommands<K> {

    private final ReactiveGraphCommands<K> reactive;

    public BlockingGraphCommandsImpl(RedisDataSource ds, ReactiveGraphCommands<K> reactive, Duration timeout) {
        super(ds, timeout);
        this.reactive = reactive;
    }

    @Override
    public void graphDelete(K key) {
        reactive.graphDelete(key).await().atMost(timeout);
    }

    @Override
    public String graphExplain(K key, String query) {
        return reactive.graphExplain(key, query).await().atMost(timeout);
    }

    @Override
    public List<K> graphList() {
        return reactive.graphList().await().atMost(timeout);
    }

    @Override
    public List<Map<String, GraphQueryResponseItem>> graphQuery(K key, String query) {
        return reactive.graphQuery(key, query).await().atMost(timeout);
    }

    @Override
    public List<Map<String, GraphQueryResponseItem>> graphQuery(K key, String query, Duration timeout) {
        return reactive.graphQuery(key, query, timeout).await().atMost(this.timeout);
    }
}
