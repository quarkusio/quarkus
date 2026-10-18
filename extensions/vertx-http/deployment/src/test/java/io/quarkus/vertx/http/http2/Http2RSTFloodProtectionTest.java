package io.quarkus.vertx.http.http2;

import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.certs.Format;
import io.smallrye.certs.junit5.Certificate;
import io.smallrye.certs.junit5.Certificates;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.Router;

/**
 * Reproduce CVE-2023-44487.
 */
@Certificates(baseDir = "target/certs", certificates = @Certificate(name = "ssl-test", password = "secret", formats = {
        Format.JKS, Format.PKCS12, Format.PEM }))
@DisabledOnOs(OS.WINDOWS)
public class Http2RSTFloodProtectionTest {

    private static final String configuration = """
            quarkus.http.ssl.certificate.key-store-file=server-keystore.jks
            quarkus.http.ssl.certificate.key-store-password=secret
            """;

    @TestHTTPResource(value = "/ping", tls = true)
    URL sslUrl;

    @TestHTTPResource(value = "/ping")
    URL url;

    @Inject
    Vertx vertx;

    private HttpClientAgent client;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new StringAsset(configuration), "application.properties")
                    .addAsResource(new File("target/certs/ssl-test-keystore.jks"), "server-keystore.jks"));

    @AfterEach
    public void closeClient() {
        if (client != null) {
            try {
                client.close().await(5, TimeUnit.SECONDS);
            } catch (TimeoutException ignored) {

            }
            client = null;
        }
    }

    @Test
    void testRstFloodProtectionWithTlsEnabled() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClientOptions options = new HttpClientOptions()
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setTrustOptions(new JksOptions().setPath(new File("target/certs/ssl-test-truststore.jks").getAbsolutePath())
                        .setPassword("secret"));

        client = vertx.httpClientBuilder()
                .with(options)
                .withConnectHandler(conn -> conn.goAwayHandler(ga -> {
                    Assertions.assertEquals(11, ga.getErrorCode());
                    latch.countDown();
                }))
                .build();
        int port = sslUrl.getPort();
        run(client, latch, port, false);
    }

    @Test
    public void testRstFloodProtection() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        HttpClientOptions options = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true);
        client = vertx.httpClientBuilder()
                .with(options)
                .withConnectHandler(conn -> conn.goAwayHandler(ga -> {
                    Assertions.assertEquals(11, ga.getErrorCode());
                    latch.countDown();
                }))
                .build();
        run(client, latch, url.getPort(), true);
    }

    void run(HttpClientAgent client, CountDownLatch latch, int port, boolean plain) throws InterruptedException {

        if (plain) {
            // Emit a first request to establish a connection.
            // It's HTTP/1 so, does not count in the number of requests.
            client.request(GET, port, "localhost", "/ping")
                    .compose(HttpClientRequest::send);
        }

        for (int i = 0; i < 250; i++) { // must be higher than the Netty limit (200 / 30s)
            client.request(GET, port, "localhost", "/ping")
                    .onSuccess(req -> req.end().onComplete(v -> req.reset()));
        }

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("RST flood protection failed");
        }
    }

    @ApplicationScoped
    public static class MyBean {

        public void register(@Observes Router router) {
            router.get("/ping").handler(rc -> {
                // Do nothing.
            });
        }

    }
}
