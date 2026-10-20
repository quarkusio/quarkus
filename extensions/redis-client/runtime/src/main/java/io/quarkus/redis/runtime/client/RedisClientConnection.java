package io.quarkus.redis.runtime.client;

import io.smallrye.common.constraint.Assert;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;

/**
 * Holds the Vert.x Redis client, its API handle, and the observable
 * metrics wrapper for a single named Redis connection.
 * <p>
 * Instances are created by the service graph during startup (one per
 * configured Redis client name) and torn down during shutdown.
 *
 * @param redis the mutiny Redis client (never {@code null})
 * @param api the mutiny Redis API (never {@code null})
 * @param observable the observable (bare) Redis client used for metrics (never {@code null})
 */
public record RedisClientConnection(Redis redis, RedisAPI api, ObservableRedis observable) {

    /**
     * Construct a new connection record.
     *
     * @param redis the mutiny Redis client (must not be {@code null})
     * @param api the mutiny Redis API (must not be {@code null})
     * @param observable the observable Redis client (must not be {@code null})
     */
    public RedisClientConnection {
        Assert.checkNotNullParam("redis", redis);
        Assert.checkNotNullParam("api", api);
        Assert.checkNotNullParam("observable", observable);
    }

    /**
     * Create a new connection from a bare Vert.x Redis client.
     *
     * @param name the client name (used for metrics labeling; must not be {@code null})
     * @param redis the bare Vert.x Redis client (must not be {@code null})
     * @param metrics the metrics observer, or {@code null} for no metrics
     * @return the new connection (never {@code null})
     */
    public static RedisClientConnection create(String name, io.vertx.redis.client.Redis redis,
            ObservableRedisMetrics metrics) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("redis", redis);
        ObservableRedis observable = new ObservableRedis(redis, name, metrics);
        Redis mutinyRedis = Redis.newInstance(observable);
        RedisAPI api = RedisAPI.api(mutinyRedis);
        return new RedisClientConnection(mutinyRedis, api, observable);
    }
}
