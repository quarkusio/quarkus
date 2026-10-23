package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that a {@code rediss://} URI enables TLS and that {@code quarkus.redis.tls.trust-all} disables the
 * verification of the (self-signed) server certificate.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsTrustAllTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://${redis.tls.host}:${redis.tls.port}")
            .overrideConfigKey("quarkus.redis.tls.trust-all", "true");

    @Inject
    RedisDataSource ds;

    @Test
    void redissSchemeWithTrustAll() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
    }
}
