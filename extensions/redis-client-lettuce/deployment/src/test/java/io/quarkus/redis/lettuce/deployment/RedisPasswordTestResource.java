package io.quarkus.redis.lettuce.deployment;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts a Redis server that requires a password ({@code requirepass}), exposing the plain endpoint as
 * {@code redis.requirepass.uri} (plus {@code redis.requirepass.host} and {@code redis.requirepass.port}) and the
 * password as {@code redis.requirepass.password}.
 */
public class RedisPasswordTestResource implements QuarkusTestResourceLifecycleManager {

    public static final String PASSWORD = "s3cr3t-p4ss";

    static GenericContainer<?> server = new GenericContainer<>(
            DockerImageName.parse("redis:8-alpine"))
            .withCommand("redis-server", "--requirepass", PASSWORD)
            .withExposedPorts(6379);

    @Override
    public Map<String, String> start() {
        server.start();
        return Map.of(
                "redis.requirepass.uri", String.format("redis://%s:%s", server.getHost(), server.getMappedPort(6379)),
                "redis.requirepass.host", server.getHost(),
                "redis.requirepass.port", String.valueOf(server.getMappedPort(6379)),
                "redis.requirepass.password", PASSWORD);
    }

    @Override
    public void stop() {
        server.stop();
    }
}
