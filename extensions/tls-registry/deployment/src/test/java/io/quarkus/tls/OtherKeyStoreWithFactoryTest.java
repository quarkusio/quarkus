package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;

import jakarta.enterprise.context.ApplicationScoped;
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
import io.smallrye.common.annotation.Identifier;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemKeyCertOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class OtherKeyStoreWithFactoryTest {

    private static final String configuration = """
            quarkus.tls.key-store.other.type=test-custom
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestKeyStoreFactory.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getKeyStoreOptions()).isNotNull();
        assertThat(def.getKeyStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getKeyStore().getCertificate("dummy-entry-0");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }

    @ApplicationScoped
    @Identifier("test-custom")
    public static class TestKeyStoreFactory implements KeyStoreFactory {
        @Override
        public KeyStoreAndKeyCertOptions createKeyStore(OtherKeyStoreConfiguration config, Vertx vertx, String name) {
            var options = new PemKeyCertOptions()
                    .addCertPath("target/certs/test-formats.crt")
                    .addKeyPath("target/certs/test-formats.key");
            try {
                return new KeyStoreAndKeyCertOptions(options.loadKeyStore(vertx), options);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
