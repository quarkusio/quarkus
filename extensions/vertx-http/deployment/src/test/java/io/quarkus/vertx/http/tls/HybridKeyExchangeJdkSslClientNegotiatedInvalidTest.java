package io.quarkus.vertx.http.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * Verifies that explicitly setting the JDK SSL engine with a client-negotiated PQC enforcement policy
 * fails at startup when the JDK does not support PQC (before JDK 27).
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-jdk-cn-invalid-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@DisabledIf("isJdk27OrLater")
public class HybridKeyExchangeJdkSslClientNegotiatedInvalidTest extends AbstractHybridKeyExchangeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(HybridKeyExchangeJdkSslClientNegotiatedInvalidTest.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-jdk-cn-invalid-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-jdk-cn-invalid-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "client-negotiated")
            .overrideConfigKey("quarkus.tls.ssl-engine", "jdkssl")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled")
            .assertException(t -> {
                Throwable cause = t;
                while (cause.getCause() != null) {
                    cause = cause.getCause();
                }
                assertThat(cause.getMessage()).contains("PQC enforcement policy");
            });

    @Test
    void shouldNotStart() {
        Assertions.fail(
                "Application should not have started with JDK SSL engine and client-negotiated PQC policy on pre-JDK 27");
    }
}
