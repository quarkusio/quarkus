package io.quarkus.redis.datasource;

import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Request;

public class DatasourceTestBase {

    final String key = UUID.randomUUID().toString();

    public static Vertx vertx;
    public static Redis redis;
    public static RedisAPI api;

    static GenericContainer<?> server = new GenericContainer<>(
            DockerImageName.parse(System.getProperty("redis.base.image", "redis:7.0.5-alpine")))
            .withExposedPorts(6379);

    @BeforeAll
    static void init() {
        vertx = Vertx.vertx();
        server.start();
        redis = Redis.createClient(vertx, "redis://" + server.getHost() + ":" + server.getFirstMappedPort());
        // If you want to use a local redis: redis = Redis.createClient(vertx, "redis://localhost:" + 6379);
        api = RedisAPI.api(redis);
    }

    @AfterAll
    static void cleanup() {
        redis.close();
        server.close();
        vertx.closeAndAwait();
    }

    public static String getRedisVersion() {
        String info = redis.send(Request.cmd(Command.INFO)).await().indefinitely().toString();
        // Look for the redis_version line
        return info.lines().filter(s -> s.startsWith("redis_version")).findAny()
                .map(line -> line.split(":")[1])
                .orElseThrow();
    }

}
