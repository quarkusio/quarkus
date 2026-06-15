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

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-ssl-options", password = "password", formats = { Format.PKCS12 })
})
public class DefaultSSLOptionsTest {

    private static final String configuration = """
            quarkus.tls.alpn=true

            quarkus.tls.cipher-suites=TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
            quarkus.tls.protocols=TLSv1.3
            quarkus.tls.handshake-timeout=20s
            quarkus.tls.session-timeout=20s


            quarkus.tls.key-store.jks.path=target/certs/test-ssl-options-keystore.p12
            quarkus.tls.key-store.jks.password=password

            quarkus.tls.trust-store.jks.path=target/certs/test-ssl-options-truststore.p12
            quarkus.tls.trust-store.jks.password=password
                """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.isTrustAll()).isFalse();
        assertThat(def.getHostnameVerificationAlgorithm()).isEmpty();

        assertThat(def.getServerSSLOptions().getKeyCertOptions()).isNotNull();
        assertThat(def.getServerSSLOptions().getTrustOptions()).isNotNull();

        assertThat(def.getServerSSLOptions().isUseAlpn()).isTrue();
        assertThat(def.getServerSSLOptions().getEnabledCipherSuites()).contains("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");

        assertThat(def.getServerSSLOptions().getEnabledSecureTransportProtocols()).containsExactly("TLSv1.3");
        assertThat(def.getServerSSLOptions().getSslHandshakeTimeoutUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(def.getServerSSLOptions().getSslHandshakeTimeout()).isEqualTo(20);
    }

}
