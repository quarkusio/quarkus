package io.quarkus.tls;

import static io.smallrye.certs.Format.PEM;
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
import io.smallrye.certs.junit5.Alias;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-sni-e2e", formats = { PEM }, aliases = {
                @Alias(name = "sni-e2e-1", cn = "localhost", subjectAlternativeNames = "dns:localhost"),
                @Alias(name = "sni-e2e-2", cn = "acme.org", subjectAlternativeNames = "dns:acme.org"),
        })
})
public class SNIEndToEndTest {

    private static final String configuration = """
            # Server with SNI
            quarkus.tls.key-store.pem.1.cert=target/certs/sni-e2e-1.crt
            quarkus.tls.key-store.pem.1.key=target/certs/sni-e2e-1.key
            quarkus.tls.key-store.pem.2.cert=target/certs/sni-e2e-2.crt
            quarkus.tls.key-store.pem.2.key=target/certs/sni-e2e-2.key
            quarkus.tls.key-store.sni=true
            """;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry registry;

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
    void testSNIServerServesCorrectCertificate() throws InterruptedException {
        TlsConfiguration tlsConfig = registry.getDefault().orElseThrow();
        assertThat(tlsConfig.usesSni()).isTrue();

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setSni(true)
                .setKeyCertOptions(tlsConfig.getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("SNI OK"))
                .listen(8081).toCompletionStage().toCompletableFuture().join();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustAll(true)
                .setVerifyHost(false)
                .setForceSni(true));

        CountDownLatch latch = new CountDownLatch(1);
        client.get(8081, "localhost", "/").send(ar -> {
            assertThat(ar.succeeded()).isTrue();
            assertThat(ar.result().bodyAsString()).isEqualTo("SNI OK");
            latch.countDown();
        });

        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }
}
