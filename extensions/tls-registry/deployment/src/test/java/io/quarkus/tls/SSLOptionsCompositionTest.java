package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.net.SSLOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-ssl-options-comp", password = "password", formats = { Format.PKCS12 })
})
public class SSLOptionsCompositionTest {

    private static final String configuration = """
            quarkus.tls.full.key-store.p12.path=target/certs/test-ssl-options-comp-keystore.p12
            quarkus.tls.full.key-store.p12.password=password
            quarkus.tls.full.trust-store.p12.path=target/certs/test-ssl-options-comp-truststore.p12
            quarkus.tls.full.trust-store.p12.password=password
            quarkus.tls.full.alpn=false
            quarkus.tls.full.cipher-suites=TLS_AES_128_GCM_SHA256,TLS_AES_256_GCM_SHA384
            quarkus.tls.full.protocols=TLSv1.3,TLSv1.2
            quarkus.tls.full.handshake-timeout=45s
            quarkus.tls.full.certificate-revocation-list=src/test/resources/revocations/revoked-cert.der

            # Minimal config with only trust store
            quarkus.tls.minimal.trust-store.p12.path=target/certs/test-ssl-options-comp-truststore.p12
            quarkus.tls.minimal.trust-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testFullSSLOptionsComposition() {
        TlsConfiguration tlsConfig = certificates.get("full").orElseThrow();
        SSLOptions options = tlsConfig.getSSLOptions();

        assertThat(options).isNotNull();
        assertThat(options.getKeyCertOptions()).isNotNull();
        assertThat(options.getTrustOptions()).isNotNull();
        assertThat(options.isUseAlpn()).isFalse();
        assertThat(options.getEnabledCipherSuites())
                .contains("TLS_AES_128_GCM_SHA256", "TLS_AES_256_GCM_SHA384");
        assertThat(options.getEnabledSecureTransportProtocols())
                .containsExactlyInAnyOrder("TLSv1.3", "TLSv1.2");
        assertThat(options.getSslHandshakeTimeout()).isEqualTo(45);
        assertThat(options.getSslHandshakeTimeoutUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(options.getCrlValues()).hasSize(1);
    }

    @Test
    void testMinimalSSLOptionsHaveDefaults() {
        TlsConfiguration tlsConfig = certificates.get("minimal").orElseThrow();
        SSLOptions options = tlsConfig.getSSLOptions();

        assertThat(options).isNotNull();
        assertThat(options.getKeyCertOptions()).isNull();
        assertThat(options.getTrustOptions()).isNotNull();

        assertThat(options.isUseAlpn()).isTrue();
        assertThat(options.getEnabledSecureTransportProtocols()).containsExactly("TLSv1.3");
        assertThat(options.getSslHandshakeTimeout()).isEqualTo(10);
        assertThat(options.getCrlValues()).isEmpty();
    }
}
