package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.Map;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;

/**
 * The REST layer answers {@code HEAD} with the {@code GET} resource method when there is no {@code HEAD} one, so a rule
 * that lists {@code GET} must count {@code HEAD} too, and in the same bucket, or {@code HEAD} would run the
 * {@code GET} method without limit.
 */
public class HeadRequestTest {

    private static final String PREFIX = "quarkus.http.rate-limit.search.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class))
            .overrideConfigKey(PREFIX + "paths", "/search")
            .overrideConfigKey(PREFIX + "methods", "GET")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Client")
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
    void headIsLimitedLikeGet() {
        Map<String, String> client = Map.of("X-Client", UUID.randomUUID().toString());
        assertThat(http.send(HttpMethod.HEAD, "/search", client, null).statusCode()).isEqualTo(200);
        assertThat(http.send(HttpMethod.HEAD, "/search", client, null).statusCode()).isEqualTo(429);
    }

    @Test
    void headAndGetShareTheBucket() {
        Map<String, String> client = Map.of("X-Client", UUID.randomUUID().toString());
        assertThat(http.get("/search", client).statusCode()).isEqualTo(200);
        assertThat(http.send(HttpMethod.HEAD, "/search", client, null).statusCode()).isEqualTo(429);
    }

    @Test
    void otherMethodsAreStillNotLimited() {
        Map<String, String> client = Map.of("X-Client", UUID.randomUUID().toString());
        for (int i = 0; i < 3; i++) {
            assertThat(http.send(HttpMethod.POST, "/search", client, io.vertx.core.buffer.Buffer.buffer("x"))
                    .statusCode()).isEqualTo(200);
        }
    }
}
