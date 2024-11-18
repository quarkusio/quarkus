package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

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

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats-encrypted-pem", password = "password", formats = { Format.JKS, Format.ENCRYPTED_PEM,
                Format.PKCS12 })
})
public class NamedEncryptedPemKeyStoreTest {

    private static final String configuration = """
            quarkus.tls.http.key-store.pem.foo.cert=target/certs/test-formats-encrypted-pem.crt
            quarkus.tls.http.key-store.pem.foo.key=target/certs/test-formats-encrypted-pem.key
            quarkus.tls.http.key-store.pem.foo.password=password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();
        TlsConfiguration named = certificates.get("http").orElseThrow();

        assertThat(def.getKeyStoreOptions()).isNull();
        assertThat(def.getKeyStore()).isNull();

        assertThat(named.getKeyStoreOptions()).isNotNull();
        assertThat(named.getKeyStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) named.getKeyStore().getCertificate("dummy-entry-0");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }
}
