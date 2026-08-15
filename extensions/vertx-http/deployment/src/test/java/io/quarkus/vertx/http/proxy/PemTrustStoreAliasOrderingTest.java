package io.quarkus.vertx.http.proxy;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.TlsConfigurationRegistry;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificates;

/**
 * Verifies that PEM truststore certificates are assigned aliases {@code cert-0}, {@code cert-1}, etc.
 * in the order they appear in the configuration. We document this ordering in the HTTP reference guide,
 * so this test ensures it holds.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @io.smallrye.certs.junit5.Certificate(name = "pem-a", formats = Format.PEM),
        @io.smallrye.certs.junit5.Certificate(name = "pem-b", formats = Format.PEM),
        @io.smallrye.certs.junit5.Certificate(name = "pem-c", formats = Format.PEM),
        @io.smallrye.certs.junit5.Certificate(name = "pem-d", formats = Format.PEM),
        @io.smallrye.certs.junit5.Certificate(name = "pem-e", formats = Format.PEM),
})
class PemTrustStoreAliasOrderingTest {

    private static final String CERTS = "target/certs/";

    private static final String configuration = """
            quarkus.tls.one.trust-store.pem.certs=%1$spem-a-ca.crt
            quarkus.tls.two.trust-store.pem.certs=%1$spem-b-ca.crt,%1$spem-a-ca.crt
            quarkus.tls.three.trust-store.pem.certs=%1$spem-c-ca.crt,%1$spem-a-ca.crt,%1$spem-b-ca.crt
            quarkus.tls.four.trust-store.pem.certs=%1$spem-d-ca.crt,%1$spem-c-ca.crt,%1$spem-b-ca.crt,%1$spem-a-ca.crt
            quarkus.tls.five.trust-store.pem.certs=%1$spem-e-ca.crt,%1$spem-d-ca.crt,%1$spem-c-ca.crt,%1$spem-b-ca.crt,%1$spem-a-ca.crt
            """
            .formatted(CERTS);

    @Inject
    TlsConfigurationRegistry registry;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar
                    .addAsResource(new StringAsset(configuration), "application.properties"));

    @Test
    void oneCert() throws Exception {
        KeyStore ts = trustStore("one");
        assertThat(ts.getCertificate("cert-0")).isEqualTo(loadCert("pem-a"));
    }

    @Test
    void twoCerts() throws Exception {
        KeyStore ts = trustStore("two");
        assertThat(ts.getCertificate("cert-0")).isEqualTo(loadCert("pem-b"));
        assertThat(ts.getCertificate("cert-1")).isEqualTo(loadCert("pem-a"));
    }

    @Test
    void threeCerts() throws Exception {
        KeyStore ts = trustStore("three");
        assertThat(ts.getCertificate("cert-0")).isEqualTo(loadCert("pem-c"));
        assertThat(ts.getCertificate("cert-1")).isEqualTo(loadCert("pem-a"));
        assertThat(ts.getCertificate("cert-2")).isEqualTo(loadCert("pem-b"));
    }

    @Test
    void fourCerts() throws Exception {
        KeyStore ts = trustStore("four");
        assertThat(ts.getCertificate("cert-0")).isEqualTo(loadCert("pem-d"));
        assertThat(ts.getCertificate("cert-1")).isEqualTo(loadCert("pem-c"));
        assertThat(ts.getCertificate("cert-2")).isEqualTo(loadCert("pem-b"));
        assertThat(ts.getCertificate("cert-3")).isEqualTo(loadCert("pem-a"));
    }

    @Test
    void fiveCerts() throws Exception {
        KeyStore ts = trustStore("five");
        assertThat(ts.getCertificate("cert-0")).isEqualTo(loadCert("pem-e"));
        assertThat(ts.getCertificate("cert-1")).isEqualTo(loadCert("pem-d"));
        assertThat(ts.getCertificate("cert-2")).isEqualTo(loadCert("pem-c"));
        assertThat(ts.getCertificate("cert-3")).isEqualTo(loadCert("pem-b"));
        assertThat(ts.getCertificate("cert-4")).isEqualTo(loadCert("pem-a"));
    }

    private KeyStore trustStore(String name) {
        return registry.get(name).orElseThrow().getTrustStore();
    }

    private static Certificate loadCert(String name) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        try (FileInputStream fis = new FileInputStream(CERTS + name + "-ca.crt")) {
            return cf.generateCertificate(fis);
        }
    }
}
