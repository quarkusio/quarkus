package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

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
import io.vertx.core.http.HttpMethod;

public class ThrowingObserverTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, ThrowingObserver.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "enforce.paths", "/enforce/*")
            .overrideConfigKey(PREFIX + "enforce.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "enforce.request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "enforce.permits", "1")
            .overrideRuntimeConfigKey(PREFIX + "enforce.window", "1m");

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
    void requestIsStillRejected() {
        assertThat(get("/enforce/x")).isEqualTo(200);
        assertThat(get("/enforce/x")).isEqualTo(429);
    }

    private int get(String path) {
        return http.send(HttpMethod.GET, path, Map.of(), null, Duration.ofSeconds(2))
                .statusCode();
    }

    @Singleton
    public static class ThrowingObserver {

        void observe(@Observes RateLimitRejectionInfo rejection) {
            throw new IllegalStateException("observer failure");
        }
    }
}
