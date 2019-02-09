package io.vertx.axle.redis;

import io.vertx.axle.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.redis.RedisOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class RedisClientTest {

    @Rule
    public GenericContainer container = new GenericContainer("redis")
            .withExposedPorts(6379);

    private Vertx vertx;

    @Before
    public void setUp() {
        vertx = Vertx.vertx();
        assertThat(vertx).isNotNull();
    }

    @After
    public void tearDown() {
        vertx.close();
    }

    @Test
    public void testAxleAPI() {
        RedisClient client = RedisClient.create(vertx, new RedisOptions()
                .setPort(container.getMappedPort(6379))
                .setHost(container.getContainerIpAddress()));

        JsonObject object = client.hset("book", "title", "The Hobbit")
                .thenCompose(x -> client.hgetall("book"))
                .toCompletableFuture()
                .join();
        assertThat(object).contains(entry("title", "The Hobbit"));
    }
}
