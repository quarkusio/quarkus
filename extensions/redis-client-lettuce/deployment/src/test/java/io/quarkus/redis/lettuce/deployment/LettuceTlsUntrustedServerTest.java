package io.quarkus.redis.lettuce.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;

/**
 * Verifies that TLS is really enforced: with a {@code rediss://} URI and no trust material configured, the
 * self-signed server certificate is rejected and the data source cannot connect at startup.
 */
@QuarkusTestResource(RedisTlsTestResource.class)
public class LettuceTlsUntrustedServerTest {

    @RegisterExtension
    static final QuarkusExtensionTest unitTest = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.redis.hosts", "rediss://${redis.tls.host}:${redis.tls.port}")
            .assertException(e -> assertThat(e).hasStackTraceContaining("SSLHandshakeException"));

    @Inject
    RedisDataSource ds;

    @Test
    void shouldNotRun() {
        // Startup should fail before this runs
    }
}
