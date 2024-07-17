package io.quarkus.vertx.http.tls.letsencrypt;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;

@Certificates(baseDir = "target/certs/lets-encrypt", certificates = {
        @Certificate(name = "self-signed", formats = { Format.PEM }), // Initial certificate
        @Certificate(name = "acme", formats = { Format.PEM }, duration = 365) // ACME certificate (fake)

})
@DisabledOnOs(OS.WINDOWS)
public class LetsEncryptFlowWithTlsConfigurationNameTest extends LetsEncryptFlowTestBase {

    public static final File temp = new File("target/acme-certificates-" + UUID.randomUUID());

    private static final String configuration = """
            # Enable SSL, configure the key store using the self-signed certificate
            quarkus.tls.http.key-store.pem.0.cert=%s/cert.pem
            quarkus.tls.http.key-store.pem.0.key=%s/key.pem
            quarkus.tls.lets-encrypt.enabled=true
            quarkus.management.enabled=true
            quarkus.http.insecure-requests=disabled
            quarkus.http.tls-configuration-name=http
            """.formatted(temp.getAbsolutePath(), temp.getAbsolutePath());

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new StringAsset((configuration)), "application.properties"))
            .overrideRuntimeConfigKey("loc", temp.getAbsolutePath())
            .setBeforeAllCustomizer(() -> {
                try {
                    // Prepare a random directory to store the certificates.
                    temp.mkdirs();
                    Files.copy(SELF_SIGNED_CERT.toPath(),
                            new File(temp, "cert.pem").toPath());
                    Files.copy(SELF_SIGNED_KEY.toPath(),
                            new File(temp, "key.pem").toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            })
            .setAfterAllCustomizer(() -> {
                try {
                    Files.deleteIfExists(new File(temp, "cert.pem").toPath());
                    Files.deleteIfExists(new File(temp, "key.pem").toPath());
                    Files.deleteIfExists(temp.toPath());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @Inject
    Vertx vertx;

    @ConfigProperty(name = "loc")
    File certs;

    @TestHTTPResource(value = "/tls", tls = true)
    String endpoint;

    @TestHTTPResource(value = "/lets-encrypt/challenge", management = true)
    String management;

    @TestHTTPResource(value = "/lets-encrypt/certs", management = true)
    String reload;

    @TestHTTPResource(value = "/.well-known/acme-challenge", tls = true)
    String challenge;

    @Test
    void testFlow() throws IOException {
        initFlow(vertx, "http");
        testLetsEncryptFlow();
    }

    @Override
    void updateCerts() throws IOException {
        // Replace the certs on disk
        Files.copy(ACME_CERT.toPath(),
                new File(certs, "cert.pem").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(ACME_KEY.toPath(),
                new File(certs, "key.pem").toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    String getApplicationEndpoint() {
        return endpoint;
    }

    @Override
    String getLetsEncryptManagementEndpoint() {
        return management;
    }

    @Override
    String getLetsEncryptCertsEndpoint() {
        return reload;
    }

    @Override
    String getChallengeEndpoint() {
        return challenge;
    }
}
