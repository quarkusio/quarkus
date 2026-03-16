package io.quarkus.vertx.http.tls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.net.URL;

import javax.net.ssl.SSLException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.OpenSSLEngineOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@EnabledIf("isOpenSslAvailable")
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-hybrid-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class HybridKeyExchangeTest {

    @TestHTTPResource(value = "/hybrid", tls = true)
    URL url;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-hybrid-test.key"), "server-key.pem")
                    .addAsResource(new File("target/certs/ssl-hybrid-test.crt"), "server-cert.pem"))
            .overrideConfigKey("quarkus.tls.key-store.pem.0.cert", "server-cert.pem")
            .overrideConfigKey("quarkus.tls.key-store.pem.0.key", "server-key.pem")
            .overrideConfigKey("quarkus.tls.hybrid", "true")
            .overrideConfigKey("quarkus.http.insecure-requests", "disabled");

    @Inject
    Vertx vertx;

    @Test
    void testHybridKeyExchangeHandshake() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setSslEngineOptions(new OpenSSLEngineOptions());
        options.setUseHybrid(true);
        options.setTrustAll(true);

        WebClient client = WebClient.create(vertx, options);
        HttpResponse<Buffer> response = client.getAbs(url.toExternalForm())
                .send().toCompletionStage().toCompletableFuture().join();
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.bodyAsString()).isEqualTo("hybrid-ok");
    }

    @Test
    void testNonHybridClientRejected() {
        WebClientOptions options = new WebClientOptions();
        options.setSsl(true);
        options.setTrustAll(true);

        WebClient client = WebClient.create(vertx, options);
        assertThatThrownBy(() -> client.getAbs(url.toExternalForm())
                .send().toCompletionStage().toCompletableFuture().join())
                .hasRootCauseInstanceOf(SSLException.class);
    }

    static boolean isOpenSslAvailable() {
        try {
            SslContext ctx = SslContextBuilder.forClient()
                    .sslProvider(SslProvider.OPENSSL)
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            router.get("/hybrid").handler(rc -> {
                assertThat(rc.request().connection().isSsl()).isTrue();
                assertThat(rc.request().isSSL()).isTrue();
                assertThat(rc.request().connection().sslSession()).isNotNull();
                assertThat(rc.request().connection().sslSession().getProtocol()).isEqualTo("TLSv1.3");
                rc.response().end("hybrid-ok");
            });
        }

    }
}
