package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.concurrent.atomic.AtomicInteger;

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
        @Certificate(name = "test-reload-A", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost"),
        @Certificate(name = "test-reload-B", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:acme.org")
})
public class ReloadOtherTrustStoreWithFactoryTest {

    private static final String configuration = """
            quarkus.tls.trust-store.other.type=test-reload
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClass(ReloadableTrustStoreFactory.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry registry;

    @Test
    void testReloading() throws CertificateParsingException, KeyStoreException {
        TlsConfiguration def = registry.getDefault().orElseThrow();

        // Initially loads cert A
        assertThat(ReloadableTrustStoreFactory.invocationCount.get()).isEqualTo(1);
        X509Certificate certificate = (X509Certificate) def.getTrustStore().getCertificate("test-reload-A");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("dns:localhost");
        });

        // Switch to cert B and reload
        ReloadableTrustStoreFactory.useCertB = true;
        assertThat(def.reload()).isTrue();
        assertThat(ReloadableTrustStoreFactory.invocationCount.get()).isEqualTo(2);

        certificate = (X509Certificate) def.getTrustStore().getCertificate("test-reload-B");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("dns:acme.org");
        });
    }

    @ApplicationScoped
    @Identifier("test-reload")
    public static class ReloadableTrustStoreFactory implements TrustStoreFactory {

        static volatile boolean useCertB = false;
        static final AtomicInteger invocationCount = new AtomicInteger(0);

        @Override
        public TrustStoreAndTrustOptions createTrustStore(OtherTrustStoreConfiguration config, Vertx vertx, String name) {
            invocationCount.incrementAndGet();
            String suffix = useCertB ? "B" : "A";
            String path = "target/certs/test-reload-" + suffix + "-truststore.p12";
            try {
                KeyStore ks = KeyStore.getInstance("PKCS12");
                try (var fis = new FileInputStream(path)) {
                    ks.load(fis, "password".toCharArray());
                }
                byte[] data = java.nio.file.Files.readAllBytes(java.nio.file.Path.of(path));
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
