package io.quarkus.tls;

import static io.smallrye.certs.Format.PEM;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.runtime.config.TlsConfigUtils;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.http.HttpClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-http-client-sni", formats = { PEM }, aliases = {
                @Alias(name = "http-sni-1", cn = "acme.org"),
                @Alias(name = "http-sni-2", cn = "example.com"),
        }),
        @Certificate(name = "test-http-client-p12", password = "password", formats = { Format.PKCS12 })
})
public class TlsConfigUtilsWithHttpClientOptionsTest {

    private static final String configuration = """
            # Configuration with SNI and hostname verification NONE
            quarkus.tls.sni-config.key-store.pem.1.cert=target/certs/http-sni-1.crt
            quarkus.tls.sni-config.key-store.pem.1.key=target/certs/http-sni-1.key
            quarkus.tls.sni-config.key-store.pem.2.cert=target/certs/http-sni-2.crt
            quarkus.tls.sni-config.key-store.pem.2.key=target/certs/http-sni-2.key
            quarkus.tls.sni-config.key-store.sni=true
            quarkus.tls.sni-config.hostname-verification-algorithm=NONE
            quarkus.tls.sni-config.cipher-suites=TLS_AES_128_GCM_SHA256
            quarkus.tls.sni-config.protocols=TLSv1.3
            quarkus.tls.sni-config.handshake-timeout=30s
            quarkus.tls.sni-config.alpn=false

            # Configuration with trust-all and HTTPS verification
            quarkus.tls.trust-all-config.trust-all=true
            quarkus.tls.trust-all-config.hostname-verification-algorithm=HTTPS

            # Configuration with key and trust store
            quarkus.tls.full-config.key-store.p12.path=target/certs/test-http-client-p12-keystore.p12
            quarkus.tls.full-config.key-store.p12.password=password
            quarkus.tls.full-config.trust-store.p12.path=target/certs/test-http-client-p12-truststore.p12
            quarkus.tls.full-config.trust-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testSniAndHostnameVerificationNone() {
        TlsConfiguration sniConfig = certificates.get("sni-config").orElseThrow();
        HttpClientOptions options = new HttpClientOptions();
        TlsConfigUtils.configure(options, sniConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.isForceSni()).isTrue();
        assertThat(options.isVerifyHost()).isFalse();
        assertThat(options.getEnabledCipherSuites()).contains("TLS_AES_128_GCM_SHA256");
        assertThat(options.getEnabledSecureTransportProtocols()).containsExactly("TLSv1.3");
        assertThat(options.getSslHandshakeTimeout()).isEqualTo(30);
        assertThat(options.isUseAlpn()).isFalse();
    }

    @Test
    void testTrustAllWithHttpsVerification() {
        TlsConfiguration trustAllConfig = certificates.get("trust-all-config").orElseThrow();
        HttpClientOptions options = new HttpClientOptions();
        TlsConfigUtils.configure(options, trustAllConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.isTrustAll()).isTrue();
        assertThat(options.isVerifyHost()).isTrue();
        assertThat(options.isForceSni()).isFalse();
    }

    @Test
    void testFullConfigWithKeyAndTrustStore() {
        TlsConfiguration fullConfig = certificates.get("full-config").orElseThrow();
        HttpClientOptions options = new HttpClientOptions();
        TlsConfigUtils.configure(options, fullConfig);

        assertThat(options.isSsl()).isTrue();
        assertThat(options.getKeyCertOptions()).isNotNull();
        assertThat(options.getTrustOptions()).isNotNull();
        assertThat(options.isTrustAll()).isFalse();
        assertThat(options.isForceSni()).isFalse();
        assertThat(options.isVerifyHost()).isTrue();
    }
}
