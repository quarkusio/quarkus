package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.enterprise.util.TypeLiteral;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.resource.ClientResources;
import io.quarkus.arc.Arc;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that injecting the Lettuce client, without a Lettuce connection, opens no connection at startup: the
 * application starts although nothing listens on the configured port, and no connection bean is registered.
 */
@QuarkusTestResource(FreePortTestResource.class)
public class LettuceClientWithoutConnectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "redis://127.0.0.1:${lettuce.test.free-port}");

    @Inject
    RedisClient client;

    @Inject
    ClientResources resources;

    @Test
    void theApplicationStartsWithoutTheServer() {
        assertThat(client).isNotNull();
        assertThat(resources).isNotNull();
        assertThat(Arc.container().select(new TypeLiteral<StatefulRedisConnection<String, String>>() {
        }).isResolvable()).isFalse();
    }

}
