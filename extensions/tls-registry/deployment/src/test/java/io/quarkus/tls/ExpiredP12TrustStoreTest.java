package io.quarkus.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;

import javax.net.ssl.SSLHandshakeException;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = {
        @Certificate(name = "expired-test-formats", password = "password", formats = { Format.JKS, Format.PEM,
                Format.PKCS12 }, duration = -5)
})
public class ExpiredP12TrustStoreTest {

    private static final String configuration = """
            # Server
            quarkus.tls.key-store.p12.path=target/certs/expired-test-formats-keystore.p12
            quarkus.tls.key-store.p12.password=password

            # Clients
            quarkus.tls.warn.trust-store.p12.path=target/certs/expired-test-formats-truststore.p12
            quarkus.tls.warn.trust-store.p12.password=password
            quarkus.tls.warn.trust-store.certificate-expiration-policy=warn

            quarkus.tls.reject.trust-store.p12.path=target/certs/expired-test-formats-truststore.p12
            quarkus.tls.reject.trust-store.p12.password=password
            quarkus.tls.reject.trust-store.certificate-expiration-policy=reject
            """;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest().setArchiveProducer(
            () -> ShrinkWrap.create(JavaArchive.class)
                    .add(new StringAsset(configuration), "application.properties"));

    @Inject
    TlsConfigurationRegistry certificates;

    @Inject
    Vertx vertx;

    @Test
    void testWarn() throws InterruptedException {
        TlsConfiguration cf = certificates.get("warn").orElseThrow();
        assertThat(cf.getTrustStoreOptions()).isNotNull();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustOptions(cf.getTrustStoreOptions()));

        vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(certificates.getDefault().orElseThrow().getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("Hello")).listen(8081).toCompletionStage().toCompletableFuture().join();

        CountDownLatch latch = new CountDownLatch(1);
        client.get(8081, "localhost", "/").send(ar -> {
            assertThat(ar.succeeded()).isTrue();
            assertThat(ar.result().bodyAsString()).isEqualTo("Hello");
            latch.countDown();
        });

        assertThat(latch.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
    }

    @Test
    void testReject() {
        TlsConfiguration cf = certificates.get("reject").orElseThrow();
        assertThat(cf.getTrustStoreOptions()).isNotNull();

        WebClient client = WebClient.create(vertx, new WebClientOptions()
                .setSsl(true)
                .setTrustOptions(cf.getTrustStoreOptions()));

        vertx.createHttpServer(new HttpServerOptions()
                .setSsl(true)
                .setKeyCertOptions(certificates.getDefault().orElseThrow().getKeyStoreOptions()))
                .requestHandler(rc -> rc.response().end("Hello")).listen(8081).toCompletionStage().toCompletableFuture().join();

        assertThatThrownBy(() -> client.get(8081, "localhost", "/")
                .send().toCompletionStage().toCompletableFuture().join()).hasCauseInstanceOf(SSLHandshakeException.class);
    }
}
