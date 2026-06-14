package io.quarkus.http3.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * Verifies that auto-TLS is skipped when a named TLS configuration is used
 * via {@code quarkus.http.tls-configuration-name}.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "http3-named-test", password = "secret", formats = {
        Format.JKS }))
class Http3NamedTlsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource(new File("target/certs/http3-named-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.my-tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.my-tls.key-store.jks.password", "secret")
            .overrideConfigKey("quarkus.http.tls-configuration-name", "my-tls");

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testAutoTlsSkippedWhenNamedTlsConfigured() {
        assertThat(registry.get("http3-dev")).isEmpty();
        assertThat(registry.get("my-tls")).isPresent();
    }
}
