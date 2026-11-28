package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.lettuce.core.RedisConnectionException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that the Lettuce connection bean, unlike the data sources, is connected at startup: an application
 * injecting it does not start when nothing listens on the configured port.
 */
@QuarkusTestResource(FreePortTestResource.class)
public class LettuceConnectionBeanStartupTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "redis://127.0.0.1:${lettuce.test.free-port}")
            // the application's classes live in the Quarkus runtime class loader: compare the class by name
            .assertException(e -> assertThat(e).hasStackTraceContaining(RedisConnectionException.class.getName()));

    @Inject
    StatefulRedisConnection<String, String> connection;

    @Test
    void shouldNotRun() {
        // the application fails to start before this runs
    }

}
