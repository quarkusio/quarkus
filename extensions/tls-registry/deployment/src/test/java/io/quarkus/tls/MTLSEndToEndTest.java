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
import io.quarkus.tls.runtime.config.TlsConfigUtils;
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
        @Certificate(name = "test-mtls-e2e", password = "password", formats = { Format.PKCS12 }, client = true)
})
public class MTLSEndToEndTest {

    private static final String configuration = """
            # Server
            quarkus.tls.server.key-store.p12.path=target/certs/test-mtls-e2e-keystore.p12
            quarkus.tls.server.key-store.p12.password=password
            quarkus.tls.server.trust-store.p12.path=target/certs/test-mtls-e2e-server-truststore.p12
            quarkus.tls.server.trust-store.p12.password=password

            # Client
            quarkus.tls.client.key-store.p12.path=target/certs/test-mtls-e2e-client-keystore.p12
            quarkus.tls.client.key-store.p12.password=password
            quarkus.tls.client.trust-store.p12.path=target/certs/test-mtls-e2e-client-truststore.p12
            quarkus.tls.client.trust-store.p12.password=password
            quarkus.tls.client.hostname-verification-algorithm=NONE
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
    void testMutualTLSHandshake() throws InterruptedException {
        TlsConfiguration serverConfig = certificates.get("server").orElseThrow();
        TlsConfiguration clientConfig = certificates.get("client").orElseThrow();

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setClientAuth(ClientAuth.REQUIRED)
                .setKeyCertOptions(serverConfig.getKeyStoreOptions())
                .setTrustOptions(serverConfig.getTrustStoreOptions()))
                .requestHandler(rc -> rc.response().end("mTLS OK"))
                .listen(8081).toCompletionStage().toCompletableFuture().join();

        WebClientOptions clientOptions = new WebClientOptions();
        TlsConfigUtils.configure(clientOptions, clientConfig);

        WebClient client = WebClient.create(vertx, clientOptions);

        CountDownLatch latch = new CountDownLatch(1);
        client.get(8081, "localhost", "/").send(ar -> {
            assertThat(ar.succeeded()).isTrue();
            assertThat(ar.result().bodyAsString()).isEqualTo("mTLS OK");
            latch.countDown();
        });

        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
}
