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
 * Verifies that auto-TLS is skipped when TLS is explicitly configured.
 */
@Certificates(baseDir = "target/certs",
        certificates = @Certificate(name = "http3-explicit-test", password = "secret", formats = {
                Format.JKS }))
class Http3ExplicitTlsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource(new File("target/certs/http3-explicit-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret");

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testAutoTlsSkippedWhenExplicitlyConfigured() {
        assertThat(registry.get("http3-dev")).isEmpty();
    }
}
