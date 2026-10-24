package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Tests that deactivating a Lettuce client via config produces a clear error.
 */
@QuarkusTestResource(RedisTestResource.class)
@SuppressWarnings("deprecation")
public class LettuceInactiveClientTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.inactive-lettuce.active", "false")
            .overrideConfigKey("quarkus.redis.inactive-lettuce.hosts", "${quarkus.redis.tr}")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Lettuce Redis Client 'inactive-lettuce' was deactivated through configuration properties"));

    @Inject
    @RedisClientName("inactive-lettuce")
    RedisClient redisClient;

    @Inject
    @RedisClientName("inactive-lettuce")
    StatefulRedisConnection<String, String> connection;

    @Test
    void shouldNotRun() {
        // Build should fail before this runs
    }
}
