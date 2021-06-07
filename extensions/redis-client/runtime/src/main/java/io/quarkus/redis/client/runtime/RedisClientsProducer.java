package io.quarkus.redis.client.runtime;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.annotation.PreDestroy;

import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.redis.client.runtime.RedisConfig.RedisConfiguration;
import io.vertx.core.Vertx;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;
import io.vertx.redis.client.RedisOptions;

public class RedisClientsProducer {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static Map<String, RedisAPIContainer> REDIS_APIS = new ConcurrentHashMap<>();

    private final Vertx vertx;
    private final RedisConfig redisConfig;

    public RedisClientsProducer(RedisConfig redisConfig, Vertx vertx) {
        this.redisConfig = redisConfig;
        this.vertx = vertx;
    }

    public RedisAPIContainer getRedisAPIContainer(String name) {
        return REDIS_APIS.computeIfAbsent(name, new Function<String, RedisAPIContainer>() {
            @Override
            public RedisAPIContainer apply(String s) {
                RedisConfiguration redisConfiguration = RedisClientUtil.getConfiguration(RedisClientsProducer.this.redisConfig,
                        name);
                Duration timeout = redisConfiguration.timeout.orElse(DEFAULT_TIMEOUT);
                RedisOptions options = RedisClientUtil.buildOptions(redisConfiguration);
                Redis redis = Redis.createClient(vertx, options);
                RedisAPI redisAPI = RedisAPI.api(redis);
                MutinyRedis mutinyRedis = new MutinyRedis(redis);
                MutinyRedisAPI mutinyRedisAPI = new MutinyRedisAPI(redisAPI);
                RedisClient redisClient = new RedisClientImpl(mutinyRedisAPI, timeout);
                ReactiveRedisClient reactiveClient = new ReactiveRedisClientImpl(mutinyRedisAPI);
                return new RedisAPIContainer(redis, redisAPI, redisClient, reactiveClient, mutinyRedis, mutinyRedisAPI);
            }
        });
    }

    public RedisClient getRedisClient(String name) {
        RedisConfiguration redisConfiguration = RedisClientUtil.getConfiguration(RedisClientsProducer.this.redisConfig,
                name);
        Duration timeout = redisConfiguration.timeout.orElse(DEFAULT_TIMEOUT);
        RedisOptions options = RedisClientUtil.buildOptions(redisConfiguration);
        Redis redis = Redis.createClient(vertx, options);
        RedisAPI redisAPI = RedisAPI.api(redis);
        MutinyRedisAPI mutinyRedisAPI = new MutinyRedisAPI(redisAPI);
        return new RedisClientImpl(mutinyRedisAPI, timeout);
    }

    public ReactiveRedisClient getReactiveRedisClient(String name) {
        RedisConfiguration redisConfiguration = RedisClientUtil.getConfiguration(RedisClientsProducer.this.redisConfig,
                name);
        RedisOptions options = RedisClientUtil.buildOptions(redisConfiguration);
        Redis redis = Redis.createClient(vertx, options);
        RedisAPI redisAPI = RedisAPI.api(redis);
        MutinyRedisAPI mutinyRedisAPI = new MutinyRedisAPI(redisAPI);
        return new ReactiveRedisClientImpl(mutinyRedisAPI);
    }

    @PreDestroy
    public void close() {
        for (RedisAPIContainer container : REDIS_APIS.values()) {
            container.close();
        }

        REDIS_APIS.clear();
    }

}
