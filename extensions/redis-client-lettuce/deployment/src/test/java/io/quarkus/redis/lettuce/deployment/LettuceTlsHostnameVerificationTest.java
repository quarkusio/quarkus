package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code quarkus.redis.tls.hostname-verification-algorithm} is applied: the server certificate is
 * issued for {@code localhost} (CN and subject alternative name), which is the host Testcontainers exposes the
 * container on, so the full peer verification succeeds.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsHostnameVerificationTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://${redis.tls.host}:${redis.tls.port}")
            .overrideConfigKey("quarkus.redis.tls.hostname-verification-algorithm", "HTTPS")
            .overrideConfigKey("quarkus.redis.tls.trust-certificate-pem.certs",
                    RedisTlsTestResource.CERTS_DIR + "/" + RedisTlsTestResource.CERT_NAME + "-client-ca.crt");

    @Inject
    RedisDataSource ds;

    @Test
    void hostnameVerified() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
    }
}
