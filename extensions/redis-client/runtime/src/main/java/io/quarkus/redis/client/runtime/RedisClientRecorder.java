package io.quarkus.redis.client.runtime;

import java.util.function.Supplier;

import io.quarkus.arc.Arc;
import io.quarkus.redis.client.RedisClient;
import io.quarkus.redis.client.reactive.ReactiveRedisClient;
import io.quarkus.runtime.annotations.Recorder;
import io.vertx.redis.client.Redis;
import io.vertx.redis.client.RedisAPI;

@Recorder
public class RedisClientRecorder {

    public Supplier<RedisClient> redisClientSupplier(String clientName) {
        return new Supplier<RedisClient>() {
            @Override
            public RedisClient get() {
                RedisAPIContainer redisAPIContainer = getRedisAPIContainer(clientName);
                return redisAPIContainer.getRedisClient();
            }
        };
    }

    public Supplier<ReactiveRedisClient> reactiveRedisClientSupplier(String clientName) {
        return new Supplier<ReactiveRedisClient>() {
            @Override
            public ReactiveRedisClient get() {
                RedisAPIContainer redisAPIContainer = getRedisAPIContainer(clientName);
                return redisAPIContainer.getReactiveClient();
            }
        };
    }

    public Supplier<MutinyRedis> mutinyRedisSupplier(String clientName) {
        return new Supplier<MutinyRedis>() {
            @Override
            public MutinyRedis get() {
                RedisAPIContainer redisAPIContainer = getRedisAPIContainer(clientName);
                return redisAPIContainer.getMutinyRedis();
            }
        };
    }

    public Supplier<MutinyRedisAPI> mutinyRedisAPISupplier(String clientName) {
        return new Supplier<MutinyRedisAPI>() {
            @Override
            public MutinyRedisAPI get() {
                RedisAPIContainer redisApiContainer = getRedisAPIContainer(clientName);
                return redisApiContainer.getMutinyRedisAPI();
            }
        };
    }

    public Supplier<RedisAPI> redisAPISupplier(String clientName) {
        return new Supplier<RedisAPI>() {
            @Override
            public RedisAPI get() {
                RedisAPIContainer redisAPIContainer = getRedisAPIContainer(clientName);
                return redisAPIContainer.getRedisAPI();
            }
        };
    }

    public Supplier<Redis> redisSupplier(String clientName) {
        return new Supplier<Redis>() {
            @Override
            public Redis get() {
                RedisAPIContainer redisAPIContainer = getRedisAPIContainer(clientName);
                return redisAPIContainer.getRedis();
            }
        };
    }

    private RedisAPIContainer getRedisAPIContainer(String clientName) {
        RedisClientsProducer redisClientsProducer = Arc.container().instance(RedisClientsProducer.class).get();
        return redisClientsProducer.getRedisAPIContainer(clientName);
    }
}
