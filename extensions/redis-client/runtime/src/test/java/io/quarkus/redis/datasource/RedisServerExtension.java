package io.quarkus.redis.datasource;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.redis.client.Command;
import io.vertx.mutiny.redis.client.Redis;
import io.vertx.mutiny.redis.client.RedisAPI;
import io.vertx.mutiny.redis.client.Request;
import io.vertx.mutiny.redis.client.Response;

@SuppressWarnings("resource")
public class RedisServerExtension implements BeforeAllCallback, AfterAllCallback {

    public static final String REDIS_DEFAULT_IMAGE = "redis/redis-stack:7.2.0-v0";
    static GenericContainer<?> server = new GenericContainer<>(
            DockerImageName.parse(System.getProperty("redis.base.image", REDIS_DEFAULT_IMAGE)))
            .withExposedPorts(6379);
    static Redis redis;
    static RedisAPI api;
    static Vertx vertx;

    public static String getHost() {
        return server.getHost();
    }

    public static Integer getFirstMappedPort() {
        return server.getFirstMappedPort();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
        init();
    }

    private static boolean init() {
        if (!server.isRunning()) {
            server.start();
            vertx = Vertx.vertx();
            redis = Redis.createClient(vertx,
                    "redis://" + RedisServerExtension.getHost() + ":" + RedisServerExtension.getFirstMappedPort());
            // If you want to use a local redis: redis = Redis.createClient(vertx, "redis://localhost:" + 6379);
            api = RedisAPI.api(redis);
            return true;
        }
        return false;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        cleanup();
    }

    private static void cleanup() {
        redis.close();
        vertx.closeAndAwait();
        server.stop();
    }

    public static List<String> getAvailableCommands() {
        boolean mustcleanup = init();
        List<String> commands = new ArrayList<>();
        Response list = redis.send(Request.cmd(Command.COMMAND)).await().indefinitely();
        for (Response response : list) {
            commands.add(response.get(0).toString());
        }
        if (mustcleanup) {
            cleanup();
        }
        return commands;

    }

    public static String getRedisVersion() {
        boolean mustcleanup = init();
        String info = redis.send(Request.cmd(Command.INFO)).await().indefinitely().toString();
        if (mustcleanup) {
            cleanup();
        }
        // Look for the redis_version line
        return info.lines().filter(s -> s.startsWith("redis_version")).findAny()
                .map(line -> line.split(":")[1])
                .orElseThrow();

    }

}
