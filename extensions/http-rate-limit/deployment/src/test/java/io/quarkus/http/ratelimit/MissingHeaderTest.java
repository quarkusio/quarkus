package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Map;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * A request without the header of a {@code header:<name>} rule is limited by its client address, like the
 * {@code principal} key limits an anonymous request: leaving the header out must not escape the limit.
 */
public class MissingHeaderTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, TestClient.class))
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Api-Key")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1h");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void requestsWithoutTheHeaderShareTheBucketOfTheirAddress() {
        assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x").statusCode()).isEqualTo(429);
        assertThat(http.get("/api/x", Map.of("X-Api-Key", " ")).statusCode()).isEqualTo(429);
        // a client that sends the header has a bucket of its own
        assertThat(http.get("/api/x", Map.of("X-Api-Key", "k1")).statusCode()).isEqualTo(200);
    }
}
