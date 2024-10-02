package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.net.SSLOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-ssl-options", password = "password", formats = { Format.PKCS12 })
})
public class NamedSSLOptionsTest {

    private static final String configuration = """
            quarkus.tls.foo.alpn=true

            quarkus.tls.foo.cipher-suites=TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256
            quarkus.tls.foo.protocols=TLSv1.3
            quarkus.tls.foo.handshake-timeout=20s
            quarkus.tls.foo.session-timeout=20s


            quarkus.tls.foo.key-store.jks.path=target/certs/test-ssl-options-keystore.p12
            quarkus.tls.foo.key-store.jks.password=password

            quarkus.tls.foo.trust-store.jks.path=target/certs/test-ssl-options-truststore.p12
            quarkus.tls.foo.trust-store.jks.password=password
                """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() {
        TlsConfiguration named = certificates.get("foo").orElseThrow();

        assertThat(named.isTrustAll()).isFalse();
        assertThat(named.getHostnameVerificationAlgorithm()).isEmpty();

        SSLOptions options = named.getSSLOptions();

        assertThat(options.getKeyCertOptions()).isNotNull();
        assertThat(options.getTrustOptions()).isNotNull();

        assertThat(options.isUseAlpn()).isTrue();
        assertThat(options.getEnabledCipherSuites()).contains("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256");

        assertThat(options.getEnabledSecureTransportProtocols()).containsExactly("TLSv1.3");
        assertThat(options.getSslHandshakeTimeoutUnit()).isEqualTo(TimeUnit.SECONDS);
        assertThat(options.getSslHandshakeTimeout()).isEqualTo(20);
    }

}
