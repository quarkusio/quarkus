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

/**
 * A disabled extension validates nothing at startup, even when Micrometer instantiates its meter binder.
 */
public class DisabledWithMicrometerTest {

    private static final String PREFIX = "quarkus.http.rate-limit.api.";

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withApplicationRoot(jar -> jar.addClasses(TestHttp.class, TestClient.class, TestHttp.Resource.class,
                    MetricsScrape.class))
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.enabled", "false")
            // invalid at startup: a rule without a positive limit, and a store timeout of zero
            .overrideRuntimeConfigKey("quarkus.http.rate-limit.store-timeout", "0s")
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "ip")
            .overrideRuntimeConfigKey(PREFIX + "permits", "0")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m");

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
    void startsAndRegistersNothing() {
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertScrape(http).doesNotHaveMetricNameContaining("http_rate_limit_requests");
    }
}
