package io.quarkus.redis.client.runtime;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

class RedisAPIContainer {
    private final Redis redis;

    private final RedisAPI redisAPI;

    private final RedisClient redisClient;

    private final ReactiveRedisClient reactiveClient;

    private final MutinyRedis mutinyRedis;

    private final MutinyRedisAPI mutinyRedisAPI;

    public RedisAPIContainer(Redis redis, RedisAPI redisAPI, RedisClient redisClient,
            ReactiveRedisClient reactiveClient, MutinyRedis mutinyRedis,
            MutinyRedisAPI mutinyRedisAPI) {
        this.redis = redis;
        this.redisAPI = redisAPI;
        this.redisClient = redisClient;
        this.reactiveClient = reactiveClient;
        this.mutinyRedis = mutinyRedis;
        this.mutinyRedisAPI = mutinyRedisAPI;
    }

    public Redis getRedis() {
        return redis;
    }

    public RedisAPI getRedisAPI() {
        return redisAPI;
    }

    public RedisClient getRedisClient() {
        return redisClient;
    }

    public ReactiveRedisClient getReactiveClient() {
        return reactiveClient;
    }

    public MutinyRedis getMutinyRedis() {
        return mutinyRedis;
    }

    public MutinyRedisAPI getMutinyRedisAPI() {
        return mutinyRedisAPI;
    }

    public void close() {
        this.redisAPI.close();
        this.redis.close();
        this.redisAPI.close();
        this.redisClient.close();
        this.reactiveClient.close();
        this.mutinyRedis.close();
        this.mutinyRedisAPI.close();
    }
}
