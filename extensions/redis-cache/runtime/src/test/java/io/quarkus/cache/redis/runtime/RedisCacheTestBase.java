package io.quarkus.cache.redis.runtime;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;

public class RedisCacheTestBase {

    final String key = UUID.randomUUID().toString();

    public static Vertx vertx;
    public static Redis redis;
    public static RedisAPI api;

    static GenericContainer<?> server = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @BeforeEach
    void init() {
        vertx = Vertx.vertx();
        server.start();
        redis = Redis.createClient(vertx, "redis://" + server.getHost() + ":" + server.getFirstMappedPort());
        // If you want to use a local redis: redis = Redis.createClient(vertx, "redis://localhost:" + 6379);
        api = RedisAPI.api(redis);
    }

    @AfterEach
    void cleanup() {
        redis.close();
        server.close();
        vertx.closeAndAwait();
    }

}
