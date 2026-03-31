package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLHandshakeException;

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
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "test-protocol-enforce", password = "password", formats = { Format.PKCS12 })
})
public class ProtocolEnforcementTest {

    private static final String configuration = """
            # Server - TLSv1.3 only
            quarkus.tls.server.key-store.p12.path=target/certs/test-protocol-enforce-keystore.p12
            quarkus.tls.server.key-store.p12.password=password
            quarkus.tls.server.protocols=TLSv1.3

            # Client with TLSv1.3 (compatible)
            quarkus.tls.client-ok.trust-store.p12.path=target/certs/test-protocol-enforce-truststore.p12
            quarkus.tls.client-ok.trust-store.p12.password=password
            quarkus.tls.client-ok.protocols=TLSv1.3

            # Client with TLSv1.2 only (should fail)
            quarkus.tls.client-fail.trust-store.p12.path=target/certs/test-protocol-enforce-truststore.p12
            quarkus.tls.client-fail.trust-store.p12.password=password
            quarkus.tls.client-fail.protocols=TLSv1.2
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
    void testTLSv13ClientSucceeds() throws InterruptedException {
        TlsConfiguration serverConfig = certificates.get("server").orElseThrow();
        TlsConfiguration clientConfig = certificates.get("client-ok").orElseThrow();

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(serverConfig.getKeyStoreOptions())
                .setEnabledSecureTransportProtocols(serverConfig.getSSLOptions().getEnabledSecureTransportProtocols()))
                .requestHandler(rc -> rc.response().end("TLSv1.3 OK"))
                .listen(8081).toCompletionStage().toCompletableFuture().join();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustOptions(clientConfig.getTrustStoreOptions())
                .setEnabledSecureTransportProtocols(clientConfig.getSSLOptions().getEnabledSecureTransportProtocols()));

        CountDownLatch latch = new CountDownLatch(1);
        client.get(8081, "localhost", "/").send(ar -> {
            assertThat(ar.succeeded()).isTrue();
            assertThat(ar.result().bodyAsString()).isEqualTo("TLSv1.3 OK");
            latch.countDown();
        });

        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testTLSv12ClientFailsAgainstTLSv13Server() {
        TlsConfiguration serverConfig = certificates.get("server").orElseThrow();
        TlsConfiguration clientConfig = certificates.get("client-fail").orElseThrow();

        server = vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(serverConfig.getKeyStoreOptions())
                .setEnabledSecureTransportProtocols(serverConfig.getSSLOptions().getEnabledSecureTransportProtocols()))
                .requestHandler(rc -> rc.response().end("Should not reach"))
                .listen(8081).toCompletionStage().toCompletableFuture().join();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustOptions(clientConfig.getTrustStoreOptions())
                .setEnabledSecureTransportProtocols(clientConfig.getSSLOptions().getEnabledSecureTransportProtocols()));

        assertThatThrownBy(() -> client.get(8081, "localhost", "/")
                .send().toCompletionStage().toCompletableFuture().join())
                .hasCauseInstanceOf(SSLHandshakeException.class);
    }
}
