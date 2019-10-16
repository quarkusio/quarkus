package io.quarkus.redis.runtime.commands;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.api.async.AsyncExecutions;
import io.lettuce.core.cluster.api.sync.Executions;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;

class QuarkusRedisExecutions<T> implements Executions<T> {
    private final AsyncExecutions<T> asyncExecutions;

    public QuarkusRedisExecutions(AsyncExecutions<T> asyncExecutions) {
        this.asyncExecutions = asyncExecutions;
    }

    @Override
    public Map<RedisClusterNode, T> asMap() {
        Map<RedisClusterNode, CompletableFuture<T>> redisClusterNodeCompletableFuture = asyncExecutions.asMap();
        Map<RedisClusterNode, T> nodeTMap = new HashMap<>();

        for (Map.Entry<RedisClusterNode, CompletableFuture<T>> entry : redisClusterNodeCompletableFuture.entrySet()) {
            nodeTMap.put(entry.getKey(), getValue(entry.getValue()));
        }

        return nodeTMap;
    }

    @Override
    public Collection<RedisClusterNode> nodes() {
        return asyncExecutions.nodes();
    }

    @Override
    public T get(RedisClusterNode redisClusterNode) {
        CompletionStage<T> tCompletionStage = asyncExecutions.get(redisClusterNode);
        return getValue(tCompletionStage.toCompletableFuture());
    }

    private T getValue(CompletableFuture<T> completionFuture) {
        try {
            return completionFuture.get(RedisURI.DEFAULT_TIMEOUT, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
