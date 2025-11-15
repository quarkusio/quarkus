package io.quarkus.vertx.http.http2;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
class Http2DisabledTest {

    protected static final String PING_DATA = "12345678";

    @TestHTTPResource(value = "/ping", tls = true)
    URL sslUrl;

    @TestHTTPResource(value = "/ping")
    URL plainUrl;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot(jar -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-file", "server-keystore.jks")
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-password", "secret")
            .overrideConfigKey("quarkus.http.http2", "false");

    @Test
    void testHttp2EnabledSsl() throws ExecutionException, InterruptedException {
        WebClientOptions options = new WebClientOptions()
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setTrustOptions(new JksOptions().setPath("target/certs/ssl-test-truststore.jks").setPassword("secret"));
        WebClient client = WebClient.create(VertxCoreRecorder.getVertx().get(), options);
        int port = sslUrl.getPort();

        runTest(client, port);
    }

    @Test
    void testHttp2EnabledPlain() throws ExecutionException, InterruptedException {
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true);
        WebClient client = WebClient.create(VertxCoreRecorder.getVertx().get(), options);
        runTest(client, plainUrl.getPort());
    }

    private void runTest(WebClient client, int port) throws InterruptedException, ExecutionException {
        CompletableFuture<HttpResponse<Buffer>> result = new CompletableFuture<>();

        client
                .get(port, "localhost", "/ping")
                .send(ar -> {
                    if (ar.succeeded()) {
                        result.complete(ar.result());
                    } else {
                        result.completeExceptionally(ar.cause());
                    }
                });

        HttpResponse<Buffer> response = result.get();
        Assertions.assertNotEquals(HttpVersion.HTTP_2, response.version());
        Assertions.assertEquals(PING_DATA, response.bodyAsString());
    }

    @ApplicationScoped
    static class MyBean {
        public void register(@Observes Router router) {
            router.get("/ping").handler(rc -> {
                rc.response().end(PING_DATA);
            });
        }
    }
}
