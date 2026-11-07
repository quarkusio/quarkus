package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.assertThat;
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

/**
 * A broad rule on {@code /*} is evaluated together with a narrower one on {@code /api/*}.
 */
public class StackedRulesMetricsTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    MetricsScrape.class))
            .overrideConfigKey("quarkus.vertx.event-loops-pool-size", "1")
            .overrideConfigKey(PREFIX + "abuse.paths", "/*")
            .overrideConfigKey(PREFIX + "abuse.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "abuse.request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "abuse.permits", "100")
            .overrideRuntimeConfigKey(PREFIX + "abuse.window", "1h")
            .overrideConfigKey(PREFIX + "narrow.paths", "/api/*")
            .overrideConfigKey(PREFIX + "narrow.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "narrow.request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "narrow.permits", "10")
            .overrideRuntimeConfigKey(PREFIX + "narrow.window", "1m");

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
    void broadRuleCountsRequestsOfNarrowPaths() {
        for (int i = 0; i < 2; i++) {
            assertThat(http.get("/api/x").statusCode()).isEqualTo(200);
        }
        assertScrape(http)
                .hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 2, entry("rule", "abuse"),
                        entry("result", "allowed"))
                .hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 2, entry("rule", "narrow"),
                        entry("result", "allowed"));
    }
}
