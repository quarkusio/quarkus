package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.builder.Version;
import io.quarkus.maven.dependency.Dependency;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that, with the Lettuce backend, the Vert.x Redis client is still created for the client of
 * {@code quarkus-redis-cache}, which looks it up at runtime, and not for the other clients.
 */
@QuarkusTestResource(RedisTestResource.class)
public class LettuceWithRedisCacheTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClass(VertxClientBeans.class))
            .setForcedDependencies(List.of(
                    Dependency.of("io.quarkus", "quarkus-redis-cache-deployment", Version.getVersion())))
            .overrideConfigKey("quarkus.redis.hosts", "${quarkus.redis.tr}")
            .overrideConfigKey("quarkus.redis.named.hosts", "${quarkus.redis.tr}");

    @Inject
    RedisDataSource ds;

    @Inject
    @RedisClientName("named")
    RedisDataSource named;

    @Test
    void theVertxClientIsCreatedForTheClientOfTheCache() {
        assertThat(VertxClientBeans.clientNames()).containsExactly(VertxClientBeans.DEFAULT);
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
        assertThat(named.execute("PING").toString()).isEqualTo("PONG");
    }
}
