package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.UUID;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.tls.runtime.keystores.ExpiryTrustOptions;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.KeyStoreOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-passwordless", password = "password", formats = { Format.PEM, Format.PKCS12 })
})
public class PasswordlessOtherTrustStoreTest {

    private static final Path tempDir = Path.of("target/test-certificates-" + UUID.randomUUID());
    private static final Path trustStorePath = tempDir.resolve("trust.p12");

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=target/certs/test-passwordless-keystore.p12
            quarkus.tls.key-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .overrideRuntimeConfigKey("quarkus.tls.passwordless.trust-store.other.type", "PKCS12")
            .overrideRuntimeConfigKey("quarkus.tls.passwordless.trust-store.other.path",
                    trustStorePath.toAbsolutePath().toString())
            .overrideRuntimeConfigKey("quarkus.tls.passwordless-alias.trust-store.other.type", "PKCS12")
            .overrideRuntimeConfigKey("quarkus.tls.passwordless-alias.trust-store.other.path",
                    trustStorePath.toAbsolutePath().toString())
            .overrideRuntimeConfigKey("quarkus.tls.passwordless-alias.trust-store.other.alias", "test-passwordless")
            .overrideRuntimeConfigKey("quarkus.tls.passwordless-ignore.trust-store.other.type", "PKCS12")
            .overrideRuntimeConfigKey("quarkus.tls.passwordless-ignore.trust-store.other.path",
                    trustStorePath.toAbsolutePath().toString())
            .overrideRuntimeConfigKey("quarkus.tls.passwordless-ignore.trust-store.certificate-expiration-policy", "ignore")
            .setBeforeAllCustomizer(() -> {
                try {
                    createPasswordlessPkcs12TrustStore(
                            Path.of("target/certs/test-passwordless.crt"),
                            "test-passwordless",
                            trustStorePath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });

    @AfterAll
    static void cleanup() throws Exception {
        if (Files.exists(tempDir)) {
            try (var walk = Files.walk(tempDir)) {
                walk.sorted((a, b) -> b.compareTo(a)).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (Exception ignored) {
                    }
                });
            }
        }
    }

    static void createPasswordlessPkcs12TrustStore(Path certPath, String alias, Path targetTrustStore) throws Exception {
        Files.createDirectories(targetTrustStore.getParent());
        Path secProps = targetTrustStore.getParent().resolve("pkcs12-passwordless.properties");
        Files.writeString(secProps,
                "keystore.pkcs12.certProtectionAlgorithm=NONE\nkeystore.pkcs12.macAlgorithm=NONE\n");

        String keytool = Path.of(System.getProperty("java.home"), "bin", "keytool").toString();
        ProcessBuilder pb = new ProcessBuilder(
                keytool,
                "-importcert",
                "-noprompt",
                "-alias", alias,
                "-file", certPath.toAbsolutePath().toString(),
                "-keystore", targetTrustStore.toAbsolutePath().toString(),
                "-storetype", "PKCS12",
                "-storepass", "changeit",
                "-J-Djava.security.properties=" + secProps.toAbsolutePath());
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (InputStream is = process.getInputStream()) {
            output = new String(is.readAllBytes());
        }
        int exitCode = process.waitFor();
        Files.deleteIfExists(secProps);
        if (exitCode != 0) {
            throw new IllegalStateException("keytool failed with exit code " + exitCode + ": " + output);
        }

        // Sanity check: confirm stock JDK loads this truststore with null password
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (InputStream is = Files.newInputStream(targetTrustStore)) {
            ks.load(is, null);
        }
        if (ks.getCertificate(alias) == null) {
            throw new IllegalStateException("Generated passwordless PKCS12 truststore is missing alias " + alias);
        }
    }

    @Inject
    TlsConfigurationRegistry certificates;

    @Inject
    Vertx vertx;

    @Test
    void testPasswordlessTrustStore() throws Exception {
        TlsConfiguration passwordless = certificates.get("passwordless").orElseThrow();

        assertThat(passwordless.getTrustStoreOptions()).isInstanceOf(ExpiryTrustOptions.class);
        ExpiryTrustOptions expiryOptions = (ExpiryTrustOptions) passwordless.getTrustStoreOptions();
        assertThat(expiryOptions.unwrap()).isInstanceOf(KeyStoreOptions.class);
        KeyStoreOptions ksOptions = (KeyStoreOptions) expiryOptions.unwrap();
        assertThat(ksOptions.getPassword()).isNull();

        assertThat(passwordless.getTrustStore()).isNotNull();
        X509Certificate certificate = (X509Certificate) passwordless.getTrustStore().getCertificate("test-passwordless");
        assertThat(certificate).isNotNull();
        assertThat(certificate.getSubjectAlternativeNames()).anySatisfy(l -> {
            assertThat(l.get(0)).isEqualTo(2);
            assertThat(l.get(1)).isEqualTo("localhost");
        });

        assertThat(passwordless.createSSLContext()).isNotNull();
        verifyHttpsHandshake(passwordless);

        assertThat(passwordless.reload()).isTrue();
        assertThat(((KeyStoreOptions) ((ExpiryTrustOptions) passwordless.getTrustStoreOptions()).unwrap()).getPassword())
                .isNull();
    }

    @Test
    void testPasswordlessTrustStoreWithAlias() throws Exception {
        TlsConfiguration withAlias = certificates.get("passwordless-alias").orElseThrow();

        assertThat(withAlias.getTrustStoreOptions()).isInstanceOf(ExpiryTrustOptions.class);
        KeyStoreOptions ksOptions = (KeyStoreOptions) ((ExpiryTrustOptions) withAlias.getTrustStoreOptions()).unwrap();
        assertThat(ksOptions.getPassword()).isNull();
        assertThat(ksOptions.getAlias()).isEqualTo("test-passwordless");

        assertThat(withAlias.getTrustStore()).isNotNull();
        assertThat(withAlias.getTrustStore().getCertificate("test-passwordless")).isNotNull();
        assertThat(withAlias.createSSLContext()).isNotNull();
        verifyHttpsHandshake(withAlias);
    }

    @Test
    void testPasswordlessTrustStoreIgnoreExpirationPolicy() throws Exception {
        TlsConfiguration ignorePolicy = certificates.get("passwordless-ignore").orElseThrow();

        assertThat(ignorePolicy.getTrustStoreOptions()).isInstanceOf(KeyStoreOptions.class);
        KeyStoreOptions ksOptions = (KeyStoreOptions) ignorePolicy.getTrustStoreOptions();
        assertThat(ksOptions.getPassword()).isNull();

        assertThat(ignorePolicy.getTrustStore()).isNotNull();
        assertThat(ignorePolicy.createSSLContext()).isNotNull();
        verifyHttpsHandshake(ignorePolicy);
    }

    private void verifyHttpsHandshake(TlsConfiguration clientConfig) {
        HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(certificates.getDefault().orElseThrow().getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("Hello"))
                .listen(0)
                .toCompletionStage()
                .toCompletableFuture()
                .join();
        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustOptions(clientConfig.getTrustStoreOptions()));
        try {
            String body = client.get(server.actualPort(), "localhost", "/")
                    .send()
                    .toCompletionStage()
                    .toCompletableFuture()
                    .join()
                    .bodyAsString();
            assertThat(body).isEqualTo("Hello");
        } finally {
            client.close();
            server.close().toCompletionStage().toCompletableFuture().join();
        }
    }
}
