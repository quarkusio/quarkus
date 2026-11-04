package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

public class MetricsTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    // obs has the shorter window, so it is evaluated before api and sees all the requests
    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    TestRateLimitClock.class, MetricsScrape.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "rules.api.paths", "/*")
            .overrideConfigKey(PREFIX + "rules.api.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "rules.api.request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "rules.api.permits", "5")
            .overrideRuntimeConfigKey(PREFIX + "rules.api.burst", "5")
            .overrideRuntimeConfigKey(PREFIX + "rules.api.window", "1m");

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
    void countersFollowDecisions() {
        for (int i = 0; i < 7; i++) {
            http.get("/x");
        }

        assertScrape(http)
                .hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 5, entry("rule", "api"),
                        entry("result", "allowed"))
                .hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 2, entry("rule", "api"),
                        entry("result", "rejected"))
                .hasMetricWithLabelsAndValue("http_rate_limit_near_limit_total", 1, entry("rule", "api"))
                .hasMetricWithLabelsAndValue("http_rate_limit_retry_after_seconds_count", 2, entry("rule", "api"),
                        entry("phase", "pre-authentication"))
                .hasMetricWithLabelsAndValueGreaterThanOrEqualTo("http_rate_limit_retry_after_seconds_max", 1.0,
                        entry("rule", "api"))
                .hasMetricWithLabelsAndValueLessThanOrEqualTo("http_rate_limit_retry_after_seconds_max", 12.0,
                        entry("rule", "api"))
                .hasMetricWithLabelsAndValue("http_rate_limit_keys", 1, entry("rule", "api"),
                        entry("phase", "pre-authentication"));
    }
}
