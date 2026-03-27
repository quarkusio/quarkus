package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
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
        @Certificate(name = "expired-mtls-ignore", password = "password", formats = {
                Format.PKCS12 }, duration = -5, client = true)
})
public class ExpiredTrustStoreWithMTLSAndIgnorePolicyTest {

    private static final String configuration = """
            # Server
            quarkus.tls.key-store.p12.path=target/certs/expired-mtls-ignore-keystore.p12
            quarkus.tls.key-store.p12.password=password
            quarkus.tls.trust-store.p12.path=target/certs/expired-mtls-ignore-server-truststore.p12
            quarkus.tls.trust-store.p12.password=password
            quarkus.tls.trust-store.certificate-expiration-policy=ignore

            # Client with IGNORE policy
            quarkus.tls.client.trust-store.p12.path=target/certs/expired-mtls-ignore-client-truststore.p12
            quarkus.tls.client.trust-store.p12.password=password
            quarkus.tls.client.trust-store.certificate-expiration-policy=ignore
            quarkus.tls.client.key-store.p12.path=target/certs/expired-mtls-ignore-client-keystore.p12
            quarkus.tls.client.key-store.p12.password=password
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
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
    void testIgnorePolicyWithMTLS() throws InterruptedException {
        TlsConfiguration clientConfig = certificates.get("client").orElseThrow();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setKeyCertOptions(clientConfig.getKeyStoreOptions())
                .setTrustOptions(clientConfig.getTrustStoreOptions()));

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setClientAuth(ClientAuth.REQUIRED)
                .setTrustOptions(certificates.getDefault().orElseThrow().getTrustStoreOptions())
                .setKeyCertOptions(certificates.getDefault().orElseThrow().getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("Hello"))
                .listen(8081).toCompletionStage().toCompletableFuture().join();

        CountDownLatch latch = new CountDownLatch(1);
        client.get(8081, "localhost", "/").send(ar -> {
            assertThat(ar.succeeded()).isTrue();
            assertThat(ar.result().bodyAsString()).isEqualTo("Hello");
            latch.countDown();
        });

        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
}
