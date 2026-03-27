package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import javax.net.ssl.SSLContext;

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
        @Certificate(name = "test-ssl-ctx", password = "password", formats = { Format.PKCS12, Format.PEM })
})
public class SSLContextCreationTest {

    private static final String configuration = """
            # Key store only
            quarkus.tls.ks-only.key-store.p12.path=target/certs/test-ssl-ctx-keystore.p12
            quarkus.tls.ks-only.key-store.p12.password=password

            # Trust store only
            quarkus.tls.ts-only.trust-store.p12.path=target/certs/test-ssl-ctx-truststore.p12
            quarkus.tls.ts-only.trust-store.p12.password=password

            # Both key store and trust store
            quarkus.tls.both.key-store.p12.path=target/certs/test-ssl-ctx-keystore.p12
            quarkus.tls.both.key-store.p12.password=password
            quarkus.tls.both.trust-store.p12.path=target/certs/test-ssl-ctx-truststore.p12
            quarkus.tls.both.trust-store.p12.password=password

            # PEM key store
            quarkus.tls.pem.key-store.pem.0.cert=target/certs/test-ssl-ctx.crt
            quarkus.tls.pem.key-store.pem.0.key=target/certs/test-ssl-ctx.key
            quarkus.tls.pem.trust-store.pem.certs=target/certs/test-ssl-ctx-ca.crt
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void testSSLContextWithKeyStoreOnly() throws Exception {
        TlsConfiguration tlsConfig = certificates.get("ks-only").orElseThrow();
        SSLContext ctx = tlsConfig.createSSLContext();
        assertThat(ctx).isNotNull();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }

    @Test
    void testSSLContextWithTrustStoreOnly() throws Exception {
        TlsConfiguration tlsConfig = certificates.get("ts-only").orElseThrow();
        SSLContext ctx = tlsConfig.createSSLContext();
        assertThat(ctx).isNotNull();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }

    @Test
    void testSSLContextWithBothStores() throws Exception {
        TlsConfiguration tlsConfig = certificates.get("both").orElseThrow();
        SSLContext ctx = tlsConfig.createSSLContext();
        assertThat(ctx).isNotNull();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
        assertThat(ctx.createSSLEngine()).isNotNull();
    }

    @Test
    void testSSLContextWithPemFormat() throws Exception {
        TlsConfiguration tlsConfig = certificates.get("pem").orElseThrow();
        SSLContext ctx = tlsConfig.createSSLContext();
        assertThat(ctx).isNotNull();
        assertThat(ctx.getProtocol()).isEqualTo("TLS");
    }
}
