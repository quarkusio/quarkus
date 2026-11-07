package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

public class MetricsDisabledTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = TestHttp.ipRule(new QuarkusExtensionTest(), "api", "/*", 1, "1m")
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    MetricsScrape.class))
            .overrideConfigKey("quarkus.http.rate-limit.metrics.enabled", "false");

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
    void noMeterAndStillLimits() {
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/x").statusCode()).isEqualTo(429);
        assertScrape(http).doesNotHaveMetricNameContaining("http_rate_limit_requests");
    }
}
