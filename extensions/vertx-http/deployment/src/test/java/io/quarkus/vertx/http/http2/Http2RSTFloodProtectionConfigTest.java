package io.quarkus.vertx.http.http2;

import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.net.URL;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.quarkus.vertx.core.runtime.VertxCoreRecorder;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.ext.web.Router;

/**
 * Configuration of the RST flood protection (CVE-2023-44487)
 */
@DisabledOnOs(OS.WINDOWS)
public class Http2RSTFloodProtectionConfigTest {

    @TestHTTPResource(value = "/ping", ssl = true)
    URL sslUrl;

    @TestHTTPResource(value = "/ping")
    URL url;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addClasses(MyBean.class)
                    .addAsResource(new File("src/test/resources/conf/ssl-jks-rst-flood-protection.conf"),
                            "application.properties")
                    .addAsResource(new File("src/test/resources/conf/server-keystore.jks"), "server-keystore.jks"));

    @Test
    void testRstFloodProtectionWithTlsEnabled() throws Exception {
        Assumptions.assumeTrue(JdkSSLEngineOptions.isAlpnAvailable()); //don't run on JDK8
        HttpClientOptions options = new HttpClientOptions()
                .setUseAlpn(true)
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setSsl(true)
                .setTrustAll(true);

        var client = VertxCoreRecorder.getVertx().get().createHttpClient(options);
        int port = sslUrl.getPort();
        run(client, port, false);
    }

    @Test
    public void testRstFloodProtection() throws InterruptedException {
        HttpClientOptions options = new HttpClientOptions()
                .setProtocolVersion(HttpVersion.HTTP_2)
                .setHttp2ClearTextUpgrade(true);
        var client = VertxCoreRecorder.getVertx().get().createHttpClient(options);
        run(client, url.getPort(), true);
    }

    void run(HttpClient client, int port, boolean plain) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        client.connectionHandler(conn -> conn.goAwayHandler(ga -> {
            Assertions.assertEquals(11, ga.getErrorCode());
            latch.countDown();
        }));

        if (plain) {
            // Emit a first request to establish a connection.
            // It's HTTP/1 so, does not count in the number of requests.
            client.request(GET, port, "localhost", "/ping")
                    .compose(HttpClientRequest::send);
        }

        for (int i = 0; i < 20; i++) {
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
