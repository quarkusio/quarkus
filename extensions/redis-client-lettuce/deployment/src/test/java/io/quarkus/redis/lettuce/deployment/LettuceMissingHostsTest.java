package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisClient;
import io.quarkus.arc.InactiveBeanException;
import io.quarkus.redis.client.RedisClientName;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Tests that a Lettuce client with no hosts configured is marked inactive.
 */
@SuppressWarnings("deprecation")
public class LettuceMissingHostsTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.devservices.enabled", "false")
            .assertException(e -> assertThat(e)
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(InactiveBeanException.class.getName()))
                    .hasMessageContainingAll(
                            "Lettuce Redis Client 'no-hosts' was deactivated automatically",
                            "the hosts are not set"));

    @Inject
    @RedisClientName("no-hosts")
    RedisClient redisClient;

    @Test
    void shouldNotRun() {
        // Build should fail before this runs
    }
}
