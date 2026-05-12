package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.security.KeyStore;
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
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.JksOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-formats", password = "password", formats = { Format.JKS, Format.PEM, Format.PKCS12 })
})
public class OtherTrustStoreWithFactoryTest {

    private static final String configuration = """
            quarkus.tls.trust-store.other.type=test-custom
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(TestTrustStoreFactory.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Test
    void test() throws KeyStoreException, CertificateParsingException {
        TlsConfiguration def = certificates.getDefault().orElseThrow();

        assertThat(def.getTrustStoreOptions()).isNotNull();
        assertThat(def.getTrustStore()).isNotNull();

        X509Certificate certificate = (X509Certificate) def.getTrustStore().getCertificate("test-formats");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });
    }

    @ApplicationScoped
    @Identifier("test-custom")
    public static class TestTrustStoreFactory implements TrustStoreFactory {
        @Override
        public TrustStoreAndTrustOptions createTrustStore(OtherTrustStoreConfiguration config, Vertx vertx, String name) {
            try {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                try (var fis = new FileInputStream("target/certs/test-formats-truststore.p12")) {
                    ks.load(fis, "password".toCharArray());
                }
                byte[] data = java.nio.file.Files.readAllBytes(
                        java.nio.file.Path.of("target/certs/test-formats-truststore.p12"));
                var options = new JksOptions()
                        .setValue(Buffer.buffer(data))
                        .setPassword("password");
                return new TrustStoreAndTrustOptions(ks, options);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
