package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that {@code hostname-verification-algorithm=NONE} of a named TLS registry configuration switches the
 * hostname verification off, as the TLS registry documents: the server certificate is issued for {@code localhost}
 * only, so connecting to {@code 127.0.0.1} succeeds only when the host name is not verified. Unlike the
 * {@code quarkus.redis.tls.*} properties, the registry passes the literal {@code NONE} on to the Vert.x options.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsRegistryNoHostnameVerificationTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://127.0.0.1:${redis.tls.port}")
            .overrideConfigKey("quarkus.redis.tls-configuration-name", "redis-tls")
            .overrideConfigKey("quarkus.tls.redis-tls.trust-store.pem.certs",
                    RedisTlsTestResource.CERTS_DIR + "/" + RedisTlsTestResource.CERT_NAME + "-client-ca.crt")
            .overrideConfigKey("quarkus.tls.redis-tls.hostname-verification-algorithm", "NONE");

    @Inject
    RedisDataSource ds;

    @Test
    void hostnameNotVerified() {
        assertThat(ds.execute("PING").toString()).isEqualTo("PONG");
    }
}
