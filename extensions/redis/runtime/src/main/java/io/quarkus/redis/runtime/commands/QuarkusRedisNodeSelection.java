package io.quarkus.redis.runtime.commands;

import java.util.HashMap;
import java.util.Map;

import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.api.async.AsyncNodeSelection;
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.NodeSelectionCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

public class QuarkusRedisNodeSelection<K, V> implements NodeSelection<K, V> {
    private final AsyncNodeSelection<K, V> asyncNodeSelection;

    public QuarkusRedisNodeSelection(AsyncNodeSelection<K, V> asyncNodeSelection) {
        this.asyncNodeSelection = asyncNodeSelection;
    }

    @Override
    public int size() {
        return this.asyncNodeSelection.size();
    }

    @Override
    public NodeSelectionCommands<K, V> commands() {
        return new QuarkusRedisNodeSelectionCommands<>(this.asyncNodeSelection.commands());
    }

    @Override
    public RedisCommands<K, V> commands(int i) {
        return new QuarkusRedisSyncCommand<>(asyncNodeSelection.commands(i));
    }

    @Override
    public RedisClusterNode node(int i) {
        return asyncNodeSelection.node(i);
    }

    @Override
    public Map<RedisClusterNode, RedisCommands<K, V>> asMap() {
        Map<RedisClusterNode, RedisAsyncCommands<K, V>> redisClusterNodeRedisAsyncCommandsMap = asyncNodeSelection.asMap();
        Map<RedisClusterNode, RedisCommands<K, V>> redisClusterNodeRedisSyncCommandsMap = new HashMap<>();

        for (Map.Entry<RedisClusterNode, RedisAsyncCommands<K, V>> entry : redisClusterNodeRedisAsyncCommandsMap.entrySet()) {
            redisClusterNodeRedisSyncCommandsMap.put(entry.getKey(), new QuarkusRedisSyncCommand<>(entry.getValue()));
        }

        return redisClusterNodeRedisSyncCommandsMap;
    }
}
