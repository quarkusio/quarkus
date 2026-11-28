package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.channel.ConnectTimeoutException;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;

/**
 * Verifies that {@code tcp.connection-timeout} bounds the connection attempts of the Lettuce backend: the data source
 * connection, opened by the first command, to an address that does not answer fails with a connect timeout after the
 * configured second, not after the ten seconds Lettuce waits by default.
 */
public class LettuceConnectionTimeoutTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            // TEST-NET-1 (RFC 5737) is not routed, so the connection attempt hangs until the timeout
            .overrideConfigKey("quarkus.redis.hosts", "redis://192.0.2.1:6379")
            .overrideConfigKey("quarkus.redis.tcp.connection-timeout", "1s");

    @Inject
    RedisDataSource ds;

    @Test
    void firstCommandFailsWithAConnectTimeout() {
        assertThatThrownBy(() -> ds.execute("PING")).rootCause()
                .isInstanceOf(ConnectTimeoutException.class)
                .hasMessageContaining("connection timed out after 1000 ms");
    }
}
