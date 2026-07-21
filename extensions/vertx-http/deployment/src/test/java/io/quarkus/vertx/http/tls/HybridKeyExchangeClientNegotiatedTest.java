package io.quarkus.vertx.http.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

import java.io.File;
import java.net.URL;
import java.util.List;

import javax.net.ssl.SSLException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-client-negotiated-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@EnabledIf("isOpenSsl35Available")
public class HybridKeyExchangeClientNegotiatedTest extends AbstractHybridKeyExchangeTest {

    @TestHTTPResource(value = "/client-negotiated", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-client-negotiated-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-client-negotiated-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.pqc-enforcement-policy", "client-negotiated")
            // Advertise both PQC and classical groups so non-PQC clients have a fallback (x25519).
            // With STRICT, x25519 alone would be rejected; CLIENT_NEGOTIATED allows it.
            .overrideConfigKey("quarkus.tls.key-exchange-groups", "X25519MLKEM768,X25519")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Test
    void testPqcClientConnects() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("X25519MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("client-negotiated-ok");
        } finally {
            client.close();
        }
    }

    @Test
    void testClientWithOnlyUnadvertisedPqcGroupFails() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("SecP256r1MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        try {
            assertThatThrownBy(() -> client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join())
                    .hasRootCauseInstanceOf(SSLException.class);
        } finally {
            client.close();
        }
    }

    @Test
    void testClientWithUnadvertisedPqcGroupFallsBackToClassical() {
        // Client prefers SecP256r1MLKEM768 (not in server's group list) but also supports X25519.
        // TLS negotiates X25519 as a common group; CLIENT_NEGOTIATED allows the classical fallback
        // whereas STRICT would reject it.
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("SecP256r1MLKEM768", "X25519"));

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("client-negotiated-ok");
        } finally {
            client.close();
        }
    }

    @Test
    @EnabledIf("isJdk27OrLater")
    void testJdkSslClientConnectsToClientNegotiatedServer() {
        // JDK 27+ supports X25519MLKEM768 natively — no OpenSSL engine needed on the client side.
        // The server still requires OpenSSL 3.5 for client-negotiated enforcement (class-level @EnabledIf).
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);
        options.getSslOptions().setKeyExchangeGroups(List.of("X25519MLKEM768"));

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("client-negotiated-ok");
        } finally {
            client.close();
        }
    }

    @Test
    void testClassicClientAlsoConnects() {
        // CLIENT_NEGOTIATED must not reject clients that don't support PQC
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);

        WebClient client = WebClient.create(vertx, options);
        try {
            HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                    .send().toCompletionStage().toCompletableFuture().join();
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.bodyAsString()).isEqualTo("client-negotiated-ok");
        } finally {
            client.close();
        }
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/client-negotiated").handler(rc -> {
                assertThat(rc.request().connection().isSsl()).isTrue();
                rc.response().end("client-negotiated-ok");
            });
        }

    }

}
