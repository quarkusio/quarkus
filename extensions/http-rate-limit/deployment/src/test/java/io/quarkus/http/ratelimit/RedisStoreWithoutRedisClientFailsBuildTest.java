package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.runtime.configuration.ConfigurationException;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * The Redis client is not part of the application, so the built-in 'redis' store does not exist.
 */
public class RedisStoreWithoutRedisClientFailsBuildTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m")
            .overrideConfigKey(PREFIX + "store", "redis")
            .assertException(t -> assertThat(t).rootCause().isInstanceOf(ConfigurationException.class)
                    .hasMessageContaining("Rate limit rule 'api' store 'redis' matches no RateLimitStore bean annotated"
                            + " @Identifier(\"redis\"); the built-in 'redis' store requires the quarkus-redis-client"
                            + " extension"));

    @Test
    void buildFails() {
        // the extension asserts the build failure
    }
}
