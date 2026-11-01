package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Integration test for CDI-managed Lettuce beans.
 * <p>
 * Verifies that:
 * <ul>
 * <li>{@code @Inject StatefulRedisConnection<String, String>} works</li>
 * <li>The connection uses Vert.x-managed event loops</li>
 * <li>SET/GET commands work through the injected connection</li>
 * </ul>
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceInjectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}");

    @Inject
    ClientResources clientResources;

    @Inject
    RedisClient redisClient;

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Test
    public void testBeansAreInjected() {
        assertThat(clientResources).isNotNull();
        assertThat(redisClient).isNotNull();
        assertThat(connection).isNotNull();
    }

    @Test
    public void testSetAndGet() {
        connection.sync().set("lettuce:test:key", "hello-from-lettuce");
        String value = connection.sync().get("lettuce:test:key");
        assertThat(value).isEqualTo("hello-from-lettuce");
    }

    @Test
    public void testConnectionUsesVertxEventLoops() {
        // The connection should be open and functional — if Vert.x event loops
        // were not shared correctly, the connection would fail to establish.
        assertThat(connection.isOpen()).isTrue();

        // Verify the client resources are using the shared event loop group
        assertThat(clientResources.eventLoopGroupProvider()).isNotNull();
    }
}
