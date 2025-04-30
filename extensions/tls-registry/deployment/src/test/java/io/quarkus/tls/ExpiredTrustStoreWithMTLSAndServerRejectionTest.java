package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.ClientAuth;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "expired-mtls", password = "password", formats = { Format.PKCS12 }, duration = -5, client = true)
})
public class ExpiredTrustStoreWithMTLSAndServerRejectionTest {

    private static final String configuration = """
            # Server
            quarkus.tls.key-store.p12.path=target/certs/expired-mtls-keystore.p12
            quarkus.tls.key-store.p12.password=password
            quarkus.tls.trust-store.p12.path=target/certs/expired-mtls-server-truststore.p12
            quarkus.tls.trust-store.p12.password=password
            quarkus.tls.trust-store.certificate-expiration-policy=reject

            # Client
            quarkus.tls.warn.trust-store.p12.path=target/certs/expired-mtls-client-truststore.p12
            quarkus.tls.warn.trust-store.p12.password=password
            quarkus.tls.warn.trust-store.certificate-expiration-policy=ignore
            quarkus.tls.warn.key-store.p12.path=target/certs/expired-mtls-client-keystore.p12
            quarkus.tls.warn.key-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Inject
    Vertx vertx;

    private HttpServer server;

    @AfterEach
    void cleanup() {
        if (server != null) {
            server.close().toCompletionStage().toCompletableFuture().join();
        }
    }

    @Test
    void testServerRejection() throws InterruptedException {
        TlsConfiguration cf = certificates.get("warn").orElseThrow();
        assertThat(cf.getTrustStoreOptions()).isNotNull();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setKeyCertOptions(cf.getKeyStoreOptions())
                .setTrustOptions(cf.getTrustStoreOptions()));

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setClientAuth(ClientAuth.REQUIRED)
                .setTrustOptions(certificates.getDefault().orElseThrow().getTrustStoreOptions())
                .setKeyCertOptions(certificates.getDefault().orElseThrow().getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("Hello")).listen(8081).toCompletionStage().toCompletableFuture().join();

        assertThatThrownBy(() -> client.get(8081, "localhost", "/").send().toCompletionStage().toCompletableFuture().join())
                .hasMessageContaining("SSLHandshakeException");
    }
}
