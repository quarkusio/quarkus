package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The Vert.x client accepts {@code valkey://} hosts; the Lettuce backend connects to Redis and rejects such a host at
 * startup with a message naming the client, the host and what to use instead.
 */
public class LettuceValkeySchemeRejectedTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "valkey://localhost:6379")
            .assertException(e -> assertThat(rootCause(e))
                    .isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("valkey://localhost:6379")
                    .hasMessageContaining("Lettuce Redis client '<default>'")
                    .hasMessageContaining("redis:// or rediss://"));

    @Inject
    RedisDataSource ds;

    @Test
    void shouldNotRun() {
        // the application fails to start before this runs
    }

    /** The startup failure may be reported as is or wrapped: the deepest cause is the one to check. */
    private static Throwable rootCause(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root;
    }
}
