package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.channel.ConnectTimeoutException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@code tcp.connection-timeout} bounds the connection attempts of the Lettuce backend: the data source
 * connection, opened at startup, to an address that does not answer fails with a connect timeout after the configured
 * second, not after the ten seconds Lettuce waits by default.
 */
public class LettuceConnectionTimeoutTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            // TEST-NET-1 (RFC 5737) is not routed, so the connection attempt hangs until the timeout
            .overrideConfigKey("quarkus.redis.hosts", "redis://192.0.2.1:6379")
            .overrideConfigKey("quarkus.redis.tcp.connection-timeout", "1s")
            // the application's classes live in the Quarkus runtime class loader: compare the class by name
            .assertException(e -> assertThat(e).rootCause()
                    .satisfies(t -> assertThat(t.getClass().getName()).isEqualTo(ConnectTimeoutException.class.getName()))
                    .hasMessageContaining("connection timed out after 1000 ms"));

    @Inject
    RedisDataSource ds;

    @Test
    void shouldNotRun() {
        // the application fails to start before this runs
    }
}
