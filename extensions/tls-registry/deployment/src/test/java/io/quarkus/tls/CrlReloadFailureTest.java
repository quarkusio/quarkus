package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;
import java.util.logging.Level;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logmanager.ExtLogRecord;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.runtime.VertxCertificateHolder;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * An unreadable certificate revocation list fails the reload, is logged, and keeps the current list.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-crl-reload-fail", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost")
})
@DisabledOnOs(OS.WINDOWS)
public class CrlReloadFailureTest {

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
            .overrideRuntimeConfigKey("quarkus.tls.certificate-revocation-list", temp.getAbsolutePath() + "/crl.der")
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .setLogRecordPredicate(r -> r.getLoggerName().equals(VertxCertificateHolder.class.getName()))
            .assertLogRecords(records -> assertThat(records)
                    .filteredOn(r -> r.getLevel().intValue() >= Level.SEVERE.intValue())
                    .singleElement().satisfies(r -> {
                        assertThat(((ExtLogRecord) r).getFormattedMessage())
                                .contains("Unable to reload the certificate revocation list");
                        assertThat(r.getThrown()).isNotNull();
                    }))
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-crl-reload-fail-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                    Files.copy(new File("src/test/resources/revocations/revoked-cert.der").toPath(),
                            new File(temp, "/crl.der").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    TlsConfigurationRegistry registry;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void testUnreadableCrlKeepsCurrentList() throws Exception {
        TlsConfiguration def = registry.getDefault().orElseThrow();
        byte[] original = Files.readAllBytes(new File(certs, "/crl.der").toPath());

        Files.delete(new File(certs, "/crl.der").toPath());

        assertThat(def.reload()).isFalse();
        assertThat(def.getServerSSLOptions().getCrlValues())
                .singleElement().satisfies(b -> assertThat(b.getBytes()).isEqualTo(original));
    }
}
