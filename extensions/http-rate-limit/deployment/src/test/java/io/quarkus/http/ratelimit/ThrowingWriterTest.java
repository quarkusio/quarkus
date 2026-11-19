package io.quarkus.http.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

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
import io.vertx.ext.web.RoutingContext;

public class ThrowingWriterTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/*", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, ThrowingWriter.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1");

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
    void throwingWriterFailsTheRequestInsteadOfHangingIt() {
        assertThat(get()).isEqualTo(200);
        assertThat(get()).isEqualTo(500);
    }

    private int get() {
        return http.send(HttpMethod.GET, "/x", Map.of(), null, Duration.ofSeconds(2))
                .statusCode();
    }

    @Singleton
    public static class ThrowingWriter implements RateLimitRejectionWriter {

        @Override
        public void write(RoutingContext context, RateLimitRejectionInfo rejection) {
            throw new IllegalStateException("writer failure");
        }
    }
}
