package io.quarkus.it.vertx;

import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@QuarkusTest
public class Http2TestCase {

    protected static final String PING_DATA = "12345678";

    @TestHTTPResource(value = "/ping", ssl = true)
    URL sslUrl;

    @TestHTTPResource(value = "/ping", ssl = false)
    URL url;

    @Test
    public void testHttp2EnabledSsl() throws ExecutionException, InterruptedException {
        runHttp2EnabledSsl("client-keystore-1.jks");
    }

    @Test
    public void testHttp2EnabledSslWithNotSelectedClientCert() throws ExecutionException, InterruptedException {
        // client-keystore-2.jks contains the key pair matching mykey-2 in server-truststore.jks,
        // but only mykey-1 is "selected" via its alias in application.properties
        ExecutionException exc = Assertions.assertThrows(ExecutionException.class,
                () -> runHttp2EnabledSsl("client-keystore-2.jks"));

        Assertions.assertEquals("SSLHandshakeException: Received fatal alert: bad_certificate",
                ExceptionUtils.getRootCauseMessage(exc));
    }

    private void runHttp2EnabledSsl(String keystoreName) throws InterruptedException, ExecutionException {
        Assumptions.assumeTrue(JdkSSLEngineOptions.isAlpnAvailable()); //don't run on JDK8
        Vertx vertx = Vertx.vertx();
        try {
            WebClientOptions options = new WebClientOptions()
                    .setUseAlpn(true)
                    .setProtocolVersion(HttpVersion.HTTP_2)
                    .setSsl(true)
                    .setKeyStoreOptions(
                            new JksOptions().setPath("src/test/resources/" + keystoreName).setPassword("password"))
                    .setTrustStoreOptions(
                            new JksOptions().setPath("src/test/resources/client-truststore.jks").setPassword("password"));

            WebClient client = WebClient.create(vertx, options);
            int port = sslUrl.getPort();

            runTest(client, port);

        } finally {
            vertx.close();
        }
    }

    @Test
    public void testHttp2EnabledPlain() throws ExecutionException, InterruptedException {
        Vertx vertx = Vertx.vertx();
        try {
            WebClientOptions options = new WebClientOptions()
                    .setProtocolVersion(HttpVersion.HTTP_2)
                    .setHttp2ClearTextUpgrade(true);
            WebClient client = WebClient.create(vertx, options);
            runTest(client, url.getPort());

        } finally {
            vertx.close();
        }
    }

    private void runTest(WebClient client, int port) throws InterruptedException, ExecutionException {
        CompletableFuture<String> result = new CompletableFuture<>();
        client
                .get(port, "localhost", "/ping")
                .virtualHost("server")
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
