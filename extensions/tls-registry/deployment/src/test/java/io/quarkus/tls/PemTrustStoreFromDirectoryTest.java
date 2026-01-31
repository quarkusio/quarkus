package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-ts-from-dir", password = "password", formats = Format.PEM)
})
class PemTrustStoreFromDirectoryTest {

    private static final String TESTED_CERT_DIR = "target/certs/test-ts-from-dir";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().withEmptyApplication()
            .withConfiguration("quarkus.tls.trust-store.pem.cert-dirs=" + TESTED_CERT_DIR)
            .setBeforeAllCustomizer(() -> {
                var caCertPath = Path.of("target/certs/test-ts-from-dir-ca.crt");
                var newTargetDir = Path.of(TESTED_CERT_DIR);
                newTargetDir.toFile().mkdirs();
                try {
                    Files.copy(caCertPath, newTargetDir.resolve("ca.pem"));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNotNull();
        assertThat(def.getTrustStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getTrustStore().getCertificate("cert-0");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }
}
