package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that, like with the Vert.x client, {@code tls.trust-all} only skips the certificate chain validation:
 * with {@code hostname-verification-algorithm=HTTPS} the host name is still verified. Here it matches the
 * certificate ({@code localhost}), so the connection succeeds without any trust material.
 * {@link LettuceTlsTrustAllHostnameMismatchTest} covers the mismatch.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsTrustAllHostnameVerificationTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://${redis.tls.host}:${redis.tls.port}")
            .overrideConfigKey("quarkus.redis.tls.trust-all", "true")
            .overrideConfigKey("quarkus.redis.tls.hostname-verification-algorithm", "HTTPS");

    @Inject
    RedisDataSource ds;

    @Test
    void trustAllWithMatchingHostname() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
    }
}
