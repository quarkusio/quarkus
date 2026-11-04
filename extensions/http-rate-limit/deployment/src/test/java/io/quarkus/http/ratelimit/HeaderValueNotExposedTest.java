package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * The value of a {@code header:<name>} key is often a credential, such as an API key. It must not reach the store, the
 * rejection event or the logs: the key holds its SHA-256 instead. This test checks the rejection event.
 */
public class HeaderValueNotExposedTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";
    private static final String SECRET = "s3cr3t-api-key";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestHttp.Resource.class, TestClient.class,
                    Observer.class))
            .overrideConfigKey(PREFIX + "paths", "/api/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Api-Key")
            .overrideRuntimeConfigKey(PREFIX + "permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "window", "1h");

    @Inject
    Vertx vertx;

    @Inject
    Observer observer;

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
    void theRejectionCarriesAHashOfTheValue() {
        Map<String, String> headers = Map.of("X-Api-Key", SECRET);
        assertThat(http.get("/api/x", headers).statusCode()).isEqualTo(200);
        assertThat(http.get("/api/x", headers).statusCode()).isEqualTo(429);

        assertThat(observer.received).singleElement().satisfies(
                rejection -> assertThat(rejection.key()).startsWith("header:sha256:").doesNotContain(SECRET));
    }

    @Singleton
    public static class Observer {

        final List<RateLimitRejectionInfo> received = new CopyOnWriteArrayList<>();

        void observe(@Observes RateLimitRejectionInfo rejection) {
            received.add(rejection);
        }
    }
}
