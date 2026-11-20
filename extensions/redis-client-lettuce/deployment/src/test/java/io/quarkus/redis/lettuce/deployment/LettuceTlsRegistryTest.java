package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code quarkus.redis.tls-configuration-name} is applied: the trust store and the key store of the
 * named TLS registry configuration are used, against a server that requires client certificates (mutual TLS) and a
 * password.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsRegistryTest {

    private static final String CERTS = RedisTlsTestResource.CERTS_DIR + "/" + RedisTlsTestResource.CERT_NAME;

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://${redis.mtls.host}:${redis.mtls.port}")
            .overrideConfigKey("quarkus.redis.password", "${redis.mtls.password}")
            .overrideConfigKey("quarkus.redis.tls-configuration-name", "redis-tls")
            .overrideConfigKey("quarkus.tls.redis-tls.trust-store.pem.certs", CERTS + "-client-ca.crt")
            .overrideConfigKey("quarkus.tls.redis-tls.key-store.pem.0.cert", CERTS + "-client.crt")
            .overrideConfigKey("quarkus.tls.redis-tls.key-store.pem.0.key", CERTS + "-client.key");

    @Inject
    RedisDataSource ds;

    @Test
    void mutualTlsWithNamedConfiguration() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
        ds.value(String.class).set("lettuce:mtls:key", "value");
        assertThat(ds.value(String.class).get("lettuce:mtls:key")).isEqualTo("value");
    }
}
