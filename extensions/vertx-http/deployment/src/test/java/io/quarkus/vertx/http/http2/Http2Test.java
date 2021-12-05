package io.quarkus.vertx.http.http2;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class Http2Test {

    protected static final String PING_DATA = "12345678";

    @TestHTTPResource(value = "/ping", ssl = true)
    URL sslUrl;

    @TestHTTPResource(value = "/ping", ssl = false)
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/ssl-jks.conf"), "application.properties")
                    .addAsResource(new File("src/test/resources/conf/server-keystore.jks"), "server-keystore.jks"));

    @Test
    public void testHttp2EnabledSsl() throws ExecutionException, InterruptedException {
        Assumptions.assumeTrue(JdkSSLEngineOptions.isAlpnAvailable()); //don't run on JDK8
        WebClientOptions options = new WebClientOptions()
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setTrustAll(true);
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
        runTest(client, url.getPort());
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
