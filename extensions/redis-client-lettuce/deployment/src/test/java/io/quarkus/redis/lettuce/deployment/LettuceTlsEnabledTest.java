package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code quarkus.redis.tls.enabled} turns on TLS for a plain {@code redis://} URI and that the
 * {@code quarkus.redis.tls.trust-certificate-pem} material is used to trust the server certificate.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsEnabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "redis://${redis.tls.host}:${redis.tls.port}")
            .overrideConfigKey("quarkus.redis.tls.enabled", "true")
            .overrideConfigKey("quarkus.redis.tls.trust-certificate-pem.certs",
                    RedisTlsTestResource.CERTS_DIR + "/" + RedisTlsTestResource.CERT_NAME + "-client-ca.crt");

    @Inject
    RedisDataSource ds;

    @Test
    void tlsEnabledByProperty() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
        ds.value(String.class).set("lettuce:tls:key", "value");
        assertThat(ds.value(String.class).get("lettuce:tls:key")).isEqualTo("value");
    }
}
