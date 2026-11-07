package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;
import java.time.Duration;
import java.util.Optional;

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
 * {@code http.rate.limit.near.limit} measures a key against its own burst: an override that gives the key a smaller
 * one than the rule's does not make every request look near the limit.
 */
public class NearLimitOverrideMetricsTest {

    private static final String PREFIX = "quarkus.http.rate-limit.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    MetricsScrape.class, SmallerBurst.class))
            .overrideConfigKey(PREFIX + "api.paths", "/*")
            .overrideConfigKey(PREFIX + "api.phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "api.request-key", "ip")
            .overrideConfigKey(PREFIX + "api.override", "smaller")
            .overrideRuntimeConfigKey(PREFIX + "api.permits", "100")
            .overrideRuntimeConfigKey(PREFIX + "api.window", "1m");

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
        // 9, 8 and 7 of the key's 10 left: none under a fifth of it, although all are under a fifth of the rule's 100
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
    public static class SmallerBurst implements RateLimitOverride {

        @Override
        public Uni<Optional<Limit>> override(RateLimitContext context, String key) {
            return Uni.createFrom().item(Optional.of(Limit.of(10, Duration.ofMinutes(1))));
        }
    }
}
