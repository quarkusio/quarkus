package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Date;
import java.util.UUID;

import jakarta.inject.Inject;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.cert.X509v2CRLBuilder;
import org.bouncycastle.operator.ContentSigner;
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
 * Verifies that a change limited to the certificate revocation list is detected, as the key store and the trust
 * store are left untouched in that case.
 */
@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-reload-crl-only", password = "password", formats = Format.PKCS12, subjectAlternativeNames = "dns:localhost")
})
@DisabledOnOs(OS.WINDOWS)
public class ReloadCrlOnlyChangeTest {

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
            .setBeforeAllCustomizer(() -> {
                try {
                    temp.mkdirs();
                    Files.copy(new File("target/certs/test-reload-crl-only-keystore.p12").toPath(),
                            new File(temp, "/tls.p12").toPath());
                    Files.write(new File(temp, "/crl.der").toPath(), generateCrl(BigInteger.ONE));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    TlsConfigurationRegistry registry;

    @ConfigProperty(name = "loc")
    File certs;

    @Test
    void testCrlOnlyChangeIsDetected() throws Exception {
        TlsConfiguration def = registry.getDefault().orElseThrow();
        assertThat(def.getServerSSLOptions().getCrlValues()).hasSize(1);

        // Reloading the exact same files is not a change.
        long initialVersion = def.getContentVersion();
        assertThat(def.reload()).isTrue();
        assertThat(def.getContentVersion()).isEqualTo(initialVersion);

        // Revoke another certificate, leaving the key store alone.
        writeCrl(BigInteger.TWO);

        assertThat(def.reload()).isTrue();
        long afterChange = def.getContentVersion();
        assertThat(afterChange).isNotEqualTo(initialVersion);
        assertThat(def.getServerSSLOptions().getCrlValues()).hasSize(1);

        // And the new list is now the current one.
        assertThat(def.reload()).isTrue();
        assertThat(def.getContentVersion()).isEqualTo(afterChange);
    }

    private void writeCrl(BigInteger revokedSerial) throws Exception {
        Files.write(new File(certs, "/crl.der").toPath(), generateCrl(revokedSerial),
                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING, java.nio.file.StandardOpenOption.WRITE);
    }

    /**
     * Builds a revocation list signed by a throwaway issuer. The registry only reads the bytes of the list, so the
     * issuer does not have to match the configured key store.
     */
    private static byte[] generateCrl(BigInteger revokedSerial) throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();

        Date now = new Date();
        X509v2CRLBuilder builder = new X509v2CRLBuilder(new X500Name("CN=test-reload-crl-only"), now);
        builder.addCRLEntry(revokedSerial, now, CRLReason.privilegeWithdrawn);
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        return builder.build(signer).getEncoded();
    }
}
