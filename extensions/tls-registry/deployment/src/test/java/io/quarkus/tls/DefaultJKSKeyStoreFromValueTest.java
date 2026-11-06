package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Base64;

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
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class DefaultJKSKeyStoreFromValueTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset("""
                            quarkus.tls.key-store.jks.value=%s
                            quarkus.tls.key-store.jks.password=password"""
                            .formatted(base64("target/certs/test-formats-keystore.jks"))), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getKeyStoreOptions()).isNotNull();
        assertThat(def.getKeyStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getKeyStore().getCertificate("test-formats");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });

        // There is no file to re-read, reloading keeps the inline value
        assertThat(def.reload()).isTrue();
        assertThat(def.getKeyStore().getCertificate("test-formats")).isNotNull();
    }

    private static String base64(String file) {
        try {
            return Base64.getEncoder().encodeToString(Files.readAllBytes(Path.of(file)));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
