package io.quarkus.redis.lettuce.deployment;

import java.util.Map;

import org.testcontainers.containers.GenericContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class RedisTestResource implements QuarkusTestResourceLifecycleManager {

    static GenericContainer<?> server = RedisImage.plainServer();

    @Override
    public Map<String, String> start() {
        server.start();
        return Map.of("quarkus.redis.tr", getEndpoint());
    }

    @Override
    public void stop() {
        server.stop();
    }

    public static String getEndpoint() {
        return String.format("redis://%s:%s", server.getHost(), server.getMappedPort(6379));
    }
}
