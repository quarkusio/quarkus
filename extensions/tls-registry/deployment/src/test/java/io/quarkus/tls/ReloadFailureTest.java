package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.KeyStoreException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.UUID;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
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
        @Certificate(name = "test-reload-fail-A", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost"),
})
public class ReloadFailureTest {

    private static final String configuration = """
            # No config
            """;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-reload-fail-A-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    TlsConfigurationRegistry registry;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void testReloadWithCorruptedFileRetainsOldCertificate()
            throws IOException, CertificateParsingException, KeyStoreException {
        TlsConfiguration def = registry.getDefault().orElseThrow();

        X509Certificate original = (X509Certificate) def.getKeyStore().getCertificate("test-reload-fail-A");
        assertThat(original).isNotNull();

        Files.write(new File(certs, "/tls.p12").toPath(), "this is not a valid p12 file".getBytes());

        assertThat(def.reload()).isFalse();

        // The previous configuration is preserved
        X509Certificate afterFailure = (X509Certificate) def.getKeyStore().getCertificate("test-reload-fail-A");
        assertThat(afterFailure).isNotNull();
        assertThat(afterFailure).isEqualTo(original);
    }
}
