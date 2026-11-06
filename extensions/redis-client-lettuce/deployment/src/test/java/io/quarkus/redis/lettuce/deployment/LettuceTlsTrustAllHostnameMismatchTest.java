package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code tls.trust-all} does not switch off an explicitly configured hostname verification: the
 * server certificate is issued for {@code localhost} only, so connecting to {@code 127.0.0.1} with
 * {@code hostname-verification-algorithm=HTTPS} fails the TLS handshake although every chain is trusted.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsTrustAllHostnameMismatchTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://127.0.0.1:${redis.tls.port}")
            .overrideConfigKey("quarkus.redis.tls.trust-all", "true")
            .overrideConfigKey("quarkus.redis.tls.hostname-verification-algorithm", "HTTPS")
            .assertException(e -> assertThat(e)
                    .hasStackTraceContaining("SSLHandshakeException")
                    .hasStackTraceContaining("No subject alternative names matching IP address 127.0.0.1"));

    @Inject
    RedisDataSource ds;

    @Test
    void shouldNotRun() {
        // Startup should fail before this runs
    }
}
