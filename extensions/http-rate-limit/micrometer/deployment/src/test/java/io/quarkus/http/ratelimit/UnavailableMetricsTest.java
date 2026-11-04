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

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;

/**
 * A request that the {@code closed} failure mode answers with 503, because the store failed, is counted with
 * {@code result=unavailable}.
 */
public class UnavailableMetricsTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp
            .rule(SwitchableStoreApp.create("closed", MetricsScrape.class), "api", "/*", "switchable");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    TestClient http;

    @BeforeEach
    void createClient() {
        http = new TestClient(vertx, url);
        SwitchableTestStore.reset();
    }

    @AfterEach
    void closeClient() {
        http.close();
    }

    @Test
    void storeFailuresAnsweredWith503AreCounted() {
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Throw());
        assertThat(http.get("/x").statusCode()).isEqualTo(503);
        assertThat(http.get("/x").statusCode()).isEqualTo(503);

        assertScrape(http).hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 2, entry("rule", "api"),
                entry("phase", "pre-authentication"), entry("result", "unavailable"));
    }
}
