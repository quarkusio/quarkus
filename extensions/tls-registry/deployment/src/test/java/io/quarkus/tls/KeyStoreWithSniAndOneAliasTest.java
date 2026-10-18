package io.quarkus.tls;

import static io.smallrye.certs.Format.JKS;
import static io.smallrye.certs.Format.PEM;
import static io.smallrye.certs.Format.PKCS12;
import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-sni-alias", password = "sni", formats = { PKCS12, JKS }, aliases = {
                @Alias(name = "sni-1", password = "sni", cn = "acme.org"),
                @Alias(name = "sni-2", password = "sni", cn = "example.com"),
        }),
        @Certificate(name = "test-sni-single", password = "sni", formats = { PKCS12, JKS,
                PEM }, subjectAlternativeNames = "DNS:localhost")
})
class KeyStoreWithSniAndOneAliasTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().withConfiguration("""
            quarkus.tls.p12-alias.key-store.p12.path=target/certs/test-sni-alias-keystore.p12
            quarkus.tls.p12-alias.key-store.p12.password=sni
            quarkus.tls.p12-alias.key-store.p12.alias-password=sni
            quarkus.tls.p12-alias.key-store.p12.alias=sni-1
            quarkus.tls.p12-alias.key-store.sni=true

            quarkus.tls.jks-single.key-store.jks.path=target/certs/test-sni-single-keystore.jks
            quarkus.tls.jks-single.key-store.jks.password=sni
            quarkus.tls.jks-single.key-store.sni=true

            quarkus.tls.pem-single.key-store.pem.a.cert=target/certs/test-sni-single.crt
            quarkus.tls.pem-single.key-store.pem.a.key=target/certs/test-sni-single.key
            quarkus.tls.pem-single.key-store.sni=true

            quarkus.tls.p12-single.key-store.p12.path=target/certs/test-sni-single-keystore.p12
            quarkus.tls.p12-single.key-store.p12.password=sni
            quarkus.tls.p12-single.key-store.sni=true
            """);

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testP12WithExplicitAliasAndSni() throws KeyStoreException {
        TlsConfiguration tlsConfiguration = registry.get("p12-alias").orElseThrow();
        assertThat(tlsConfiguration.usesSni()).isTrue();
        assertThat(tlsConfiguration.getKeyStore().size()).isEqualTo(1);
    }

    @Test
    void testP12SingleAliasWithSni() throws KeyStoreException {
        TlsConfiguration tlsConfiguration = registry.get("p12-single").orElseThrow();
        assertThat(tlsConfiguration.usesSni()).isTrue();
        assertThat(tlsConfiguration.getKeyStore().size()).isEqualTo(1);
    }

    @Test
    void testJksSingleAliasWithSni() throws KeyStoreException {
        TlsConfiguration tlsConfiguration = registry.get("jks-single").orElseThrow();
        assertThat(tlsConfiguration.usesSni()).isTrue();
        assertThat(tlsConfiguration.getKeyStore().size()).isEqualTo(1);
    }

    @Test
    void testPemSingleCertWithSni() throws KeyStoreException {
        TlsConfiguration tlsConfiguration = registry.get("pem-single").orElseThrow();
        assertThat(tlsConfiguration.usesSni()).isTrue();
        assertThat(tlsConfiguration.getKeyStore().size()).isEqualTo(1);
    }
}
