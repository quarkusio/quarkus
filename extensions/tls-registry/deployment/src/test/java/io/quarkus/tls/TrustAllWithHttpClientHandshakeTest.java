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
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-trust-all-handshake", password = "password", formats = { Format.PKCS12 })
})
public class TrustAllWithHttpClientHandshakeTest {

    private static final String configuration = """
            # Server
            quarkus.tls.server.key-store.p12.path=target/certs/test-trust-all-handshake-keystore.p12
            quarkus.tls.server.key-store.p12.password=password

            # Client with trust-all
            quarkus.tls.client.trust-all=true
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
    void testTrustAllClientHandshake() throws InterruptedException {
        TlsConfiguration serverConfig = certificates.get("server").orElseThrow();
        TlsConfiguration clientConfig = certificates.get("client").orElseThrow();

        assertThat(clientConfig.isTrustAll()).isTrue();

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(serverConfig.getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("Trust All OK"))
                .listen(8081).toCompletionStage().toCompletableFuture().join();

        WebClientOptions clientOptions = new WebClientOptions();
        TlsConfigUtils.configure(clientOptions, clientConfig);

        WebClient client = WebClient.create(vertx, clientOptions);

        CountDownLatch latch = new CountDownLatch(1);
        client.get(8081, "localhost", "/").send(ar -> {
            assertThat(ar.succeeded()).isTrue();
            assertThat(ar.result().bodyAsString()).isEqualTo("Trust All OK");
            latch.countDown();
        });

        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
}
