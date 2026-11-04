package io.quarkus.http.ratelimit;

import java.net.URL;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.Http2ClientConfig;
import io.vertx.core.http.HttpClientAgent;
import io.vertx.core.http.HttpClientConfig;
import io.vertx.core.http.HttpVersion;

/**
 * The limiter over cleartext HTTP/2, where one connection carries many requests: a refused request is answered like
 * over HTTP/1.1, and one whose body exceeds {@code max-body-size} ends its own stream, not the connection.
 */
public class Http2RejectionTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/api/*", 1, "1h")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class,
                    MultiplexedRejections.class, MultiplexedRejections.SlowRoute.class))
            .overrideConfigKey("quarkus.http.limits.max-body-size", "1K");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    HttpClientAgent client;

    @BeforeEach
    void createClient() {
        // prior knowledge: HTTP/2 from the first request, on one connection
        client = vertx.httpClientBuilder().with(new HttpClientConfig().setVersions(HttpVersion.HTTP_2)
                .setHttp2Config(new Http2ClientConfig().setClearTextUpgrade(false))).build();
    }

    @AfterEach
    void closeClient() throws TimeoutException {
        client.close().await(5, TimeUnit.SECONDS);
    }

    @Test
    void rejectionIsAnswered() throws TimeoutException {
        MultiplexedRejections.rejectionIsAnswered(client, url.getPort(), HttpVersion.HTTP_2);
    }

    @Test
    void declaredBodyOverTheMaxBodySizeEndsOnlyItsOwnStream() throws TimeoutException {
        MultiplexedRejections.declaredBodyEndsOnlyItsOwnStream(client, url.getPort());
    }

    @Test
    void streamedBodyOverTheMaxBodySizeEndsOnlyItsOwnStream() throws TimeoutException {
        MultiplexedRejections.streamedBodyEndsOnlyItsOwnStream(client, url.getPort());
    }
}
