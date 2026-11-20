package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that the Vert.x Redis client is not created for the clients used only through their data sources or
 * through the Lettuce types: the Lettuce backend serves them, so nothing needs a Vert.x client for them.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceDataSourceWithoutVertxClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxClientBeans.class))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.named.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.lettuce-only.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Inject
    ReactiveRedisDataSource reactive;

    @Inject
    @RedisClientName("named")
    RedisDataSource named;

    @Inject
    @RedisClientName("lettuce-only")
    RedisClient lettuceOnly;

    @Test
    void theDataSourcesWork() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
        assertThat(reactive.execute("PING").await().atMost(Duration.ofSeconds(10)).toString()).isEqualTo("PONG");
        assertThat(named.execute("PING").toString()).isEqualTo("PONG");
        try (StatefulRedisConnection<String, String> connection = lettuceOnly.connect()) {
            assertThat(connection.sync().ping()).isEqualTo("PONG");
        }
    }

    @Test
    void noVertxClientIsCreated() {
        assertThat(VertxClientBeans.clientNames()).isEmpty();
    }
}
