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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class Http2Test {

    protected static final String PING_DATA = "12345678";

    @TestHTTPResource(value = "/ping", ssl = true)
    URL sslUrl;

    @TestHTTPResource(value = "/ping")
    URL plainUrl;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-file", "server-keystore.jks")
            .overrideConfigKey("quarkus.http.ssl.certificate.key-store-password", "secret");

    @Test
    public void testHttp2EnabledSsl() throws ExecutionException, InterruptedException {
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
    public void testHttp2EnabledPlain() throws ExecutionException, InterruptedException {
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true);
        WebClient client = WebClient.create(VertxCoreRecorder.getVertx().get(), options);
        runTest(client, plainUrl.getPort());
    }

    private void runTest(WebClient client, int port) throws InterruptedException, ExecutionException {
        CompletableFuture<String> result = new CompletableFuture<>();
        client
                .get(port, "localhost", "/ping")
                .send(ar -> {
                    if (ar.succeeded()) {
                        // Obtain response
                        HttpResponse<Buffer> response = ar.result();
                        result.complete(response.bodyAsString());
                    } else {
                        result.completeExceptionally(ar.cause());
                    }
                });
        Assertions.assertEquals(PING_DATA, result.get());
    }

    @ApplicationScoped
    static class MyBean {

        public void register(@Observes Router router) {
            //ping only works on HTTP/2
            router.get("/ping").handler(rc -> {
                rc.request().connection().ping(Buffer.buffer(PING_DATA), new Handler<AsyncResult<Buffer>>() {
                    @Override
                    public void handle(AsyncResult<Buffer> event) {
                        rc.response().end(event.result());
                    }
                });
            });
        }

    }
}
