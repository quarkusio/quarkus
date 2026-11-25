package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;

/**
 * A change limited to the certificate revocation list, with the key store left untouched, must be notified.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-periodic-crl", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost")
})
@DisabledOnOs(OS.WINDOWS)
public class PeriodicReloadWithCRLChangeTest {

    private static final String configuration = """
            # No config
            """;

    public static final File temp = new File("target/test-certificates-" + UUID.randomUUID());

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .addClasses(CertificateUpdateObserver.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.path", temp.getAbsolutePath() + "/tls.p12")
            .overrideRuntimeConfigKey("quarkus.tls.key-store.p12.password", "password")
            .overrideRuntimeConfigKey("quarkus.tls.certificate-revocation-list", temp.getAbsolutePath() + "/crl.der")
            .overrideRuntimeConfigKey("quarkus.tls.reload-period", "1s")
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-periodic-crl-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                    Files.copy(new File("src/test/resources/revocations/revoked-cert.der").toPath(),
                            new File(temp, "/crl.der").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    TlsConfigurationRegistry registry;

    @Inject
    CertificateUpdateObserver observer;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void testCrlOnlyChangeFiresEvent() throws Exception {
        TlsConfiguration def = registry.getDefault().orElseThrow();
        byte[] newCrl = generateCrl();

        await().during(2, TimeUnit.SECONDS).atMost(4, TimeUnit.SECONDS)
                .until(() -> observer.events().isEmpty());

        File tmp = new File(certs, "/crl.der.tmp");
        Files.write(tmp.toPath(), newCrl);
        Files.move(tmp.toPath(), new File(certs, "/crl.der").toPath(), StandardCopyOption.REPLACE_EXISTING);

        await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(observer.events()).hasSize(1));
        assertThat(def.getServerSSLOptions().getCrlValues())
                .singleElement().satisfies(b -> assertThat(b.getBytes()).isEqualTo(newCrl));
    }

    /**
     * The registry only reads the bytes of the list, so a throwaway issuer is enough.
     */
    private static byte[] generateCrl() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        Date now = new Date();
        X509v2CRLBuilder builder = new X509v2CRLBuilder(new X500Name("CN=test-periodic-crl"), now);
        builder.addCRLEntry(BigInteger.TWO, now, CRLReason.privilegeWithdrawn);
        return builder.build(new JcaContentSignerBuilder("SHA256withRSA").build(generator.generateKeyPair().getPrivate()))
                .getEncoded();
    }

    @ApplicationScoped
    public static class CertificateUpdateObserver {

        private final CopyOnWriteArrayList<CertificateUpdatedEvent> receivedEvents = new CopyOnWriteArrayList<>();

        public void onCertificateUpdate(@Observes CertificateUpdatedEvent event) {
            receivedEvents.add(event);
        }

        public CopyOnWriteArrayList<CertificateUpdatedEvent> events() {
            return receivedEvents;
        }
    }
}
