package io.quarkus.vertx.http.http2;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
public class Http2WithNamedConfigTlsRegistryTest {

    protected static final String PING_DATA = "87654321";

    @TestHTTPResource(value = "/ping", tls = true)
    URL tlsUrl;

    @TestHTTPResource(value = "/ping")
    URL plainUrl;

    @Inject
    Vertx vertx;

    private WebClient client;
    private HttpClient httpClient;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "server-keystore.jks"))
            .overrideConfigKey("quarkus.tls.key-store.jks.path", "server-keystore.jks")
            .overrideConfigKey("quarkus.tls.key-store.jks.password", "secret");

    @AfterEach
    public void closeClient() throws TimeoutException {
        if (client != null) {
            httpClient.close().await(5, TimeUnit.SECONDS); // we don't use client.close() because that doesn't wait
            httpClient = null;
            client = null;
        }
    }

    @Test
    public void testHttp2EnabledTls() throws ExecutionException, InterruptedException {
        WebClientOptions options = new WebClientOptions()
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setTrustOptions(new JksOptions().setPath("target/certs/ssl-test-truststore.jks").setPassword("secret"));
        httpClient = vertx.createHttpClient(options);
        client = WebClient.wrap(httpClient, options);
        int port = tlsUrl.getPort();

        runTest(client, port);
    }

    @Test
    public void testHttp2EnabledPlain() throws ExecutionException, InterruptedException {
        WebClientOptions options = new WebClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true);
        httpClient = vertx.createHttpClient(options);
        client = WebClient.wrap(httpClient, options);
        runTest(client, plainUrl.getPort());
    }

    private void runTest(WebClient client, int port) throws InterruptedException, ExecutionException {
        CompletableFuture<String> result = new CompletableFuture<>();
        client
                .get(port, "localhost", "/ping")
                .send().onComplete(ar -> {
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
                rc.request().connection().ping(Buffer.buffer(PING_DATA)).onComplete(event -> {
                    rc.response().end(event.result());
                });
            });
        }

    }
}
