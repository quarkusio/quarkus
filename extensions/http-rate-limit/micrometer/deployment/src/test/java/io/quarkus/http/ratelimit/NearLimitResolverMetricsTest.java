package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.smallrye.common.annotation.Identifier;
import io.smallrye.mutiny.Uni;
import io.vertx.core.Vertx;

/**
 * {@code http.rate.limit.near.limit} measures a key against the burst selected by its limit resolver.
 */
public class NearLimitResolverMetricsTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    MetricsScrape.class, SmallerBurst.class))
            .overrideConfigKey(PREFIX + "rules.api.paths", "/*")
            .overrideConfigKey(PREFIX + "rules.api.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "rules.api.request-key", "ip")
            .overrideConfigKey(PREFIX + "rules.api.limit-resolver", "smaller");

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
    void nearLimitUsesTheBurstOfTheKey() {
        // 9, 8 and 7 of the selected limit's 10 left: none under a fifth of it.
        for (int i = 0; i < 3; i++) {
            http.get("/x");
        }

        assertScrape(http)
                .hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 3, entry("rule", "api"),
                        entry("result", "allowed"))
                .hasMetricWithLabelsAndValue("http_rate_limit_near_limit_total", 0, entry("rule", "api"));
    }

    @Singleton
    @Identifier("smaller")
    public static class SmallerBurst implements RateLimitResolver {

        @Override
        public Uni<RateLimit> resolve(RateLimitContext context) {
            return Uni.createFrom().item(RateLimit.of(10, Duration.ofMinutes(1)));
        }
    }
}
