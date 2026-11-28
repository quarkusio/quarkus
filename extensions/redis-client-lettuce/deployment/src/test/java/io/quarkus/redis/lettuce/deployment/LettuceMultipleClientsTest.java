package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Tests that two named Lettuce clients can coexist and share ClientResources.
 */
@QuarkusTestResource(RedisTestResource.class)
@SuppressWarnings("deprecation")
public class LettuceMultipleClientsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.client-a.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.client-b.hosts", "${quarkus.redis.tr}");

    @Inject
    StatefulRedisConnection<String, String> defaultConnection;

    @Inject
    @RedisClientName("client-a")
    StatefulRedisConnection<String, String> connectionA;

    @Inject
    @RedisClientName("client-b")
    StatefulRedisConnection<String, String> connectionB;

    @Inject
    ClientResources clientResources;

    @Test
    public void testAllConnectionsWork() {
        assertThat(defaultConnection).isNotNull();
        assertThat(connectionA).isNotNull();
        assertThat(connectionB).isNotNull();
        assertThat(clientResources).isNotNull();
    }

    @Test
    public void testClientsAreIndependent() {
        defaultConnection.sync().set("lettuce:multi:default", "default-val");
        connectionA.sync().set("lettuce:multi:a", "a-val");
        connectionB.sync().set("lettuce:multi:b", "b-val");

        assertThat(defaultConnection.sync().get("lettuce:multi:default")).isEqualTo("default-val");
        assertThat(connectionA.sync().get("lettuce:multi:a")).isEqualTo("a-val");
        assertThat(connectionB.sync().get("lettuce:multi:b")).isEqualTo("b-val");
    }

    @Test
    public void testSharedClientResources() {
        // All clients share the same ClientResources (and thus Vert.x event loops)
        assertThat(clientResources.eventLoopGroupProvider()).isNotNull();
    }
}
