package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.TrustOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-passwordless", password = "password", formats = { Format.PEM, Format.PKCS12 })
})
// Storing a PKCS12 store with a null password (no encryption, no integrity check) requires JDK 18+
@EnabledForJreRange(min = JRE.JAVA_18)
public class PasswordlessTrustStoreTest {

    private static final Path PASSWORDLESS_TRUST_STORE = Path.of("target/certs/test-passwordless-nopassword-truststore.p12");

    private static final String configuration = """
            quarkus.tls.key-store.p12.path=target/certs/test-passwordless-keystore.p12
            quarkus.tls.key-store.p12.password=password

            quarkus.tls.p12.trust-store.p12.path=target/certs/test-passwordless-nopassword-truststore.p12

            quarkus.tls.other.trust-store.other.type=PKCS12
            quarkus.tls.other.trust-store.other.path=target/certs/test-passwordless-nopassword-truststore.p12
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"))
            .setBeforeAllCustomizer(PasswordlessTrustStoreTest::createPasswordlessTrustStore);

    static void createPasswordlessTrustStore() {
        try (InputStream in = Files.newInputStream(Path.of("target/certs/test-passwordless.crt"));
                OutputStream out = Files.newOutputStream(PASSWORDLESS_TRUST_STORE)) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(null, null);
            ks.setCertificateEntry("test-passwordless", CertificateFactory.getInstance("X.509").generateCertificate(in));
            // A null password stores the certificates unencrypted and without integrity check (JDK 18+)
            ks.store(out, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Inject
    TlsConfigurationRegistry certificates;

    @Inject
    Vertx vertx;

    @Test
    void testP12() throws Exception {
        verify(certificates.get("p12").orElseThrow());
    }

    @Test
    void testOther() throws Exception {
        verify(certificates.get("other").orElseThrow());
    }

    private void verify(TlsConfiguration tls) throws Exception {
        assertThat(tls.getTrustStore().getCertificate("test-passwordless")).isNotNull();
        verifyHandshake(tls.getTrustStoreOptions());
        assertThat(tls.reload()).isTrue();
        verifyHandshake(tls.getTrustStoreOptions());
    }

    private void verifyHandshake(TrustOptions trustOptions) {
        // Vert.x reloads the store from the options, so this also checks it accepts a null password
        HttpServer server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(certificates.getDefault().orElseThrow().getKeyStoreOptions()))
                .requestHandler(req -> req.response().end("hello"))
                .listen(0).await();
        HttpClient client = vertx.createHttpClient(new HttpClientOptions().setSsl(true).setTrustOptions(trustOptions));
        try {
            String body = client.request(HttpMethod.GET, server.actualPort(), "localhost", "/")
                    .compose(req -> req.send().compose(HttpClientResponse::body))
                    .await().toString();
            assertThat(body).isEqualTo("hello");
        } finally {
            client.close().await();
            server.close().await();
        }
    }
}
