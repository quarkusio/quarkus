package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.LoadSheddingInteractionApp.status;
import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import jakarta.inject.Inject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

/**
 * Load shedding runs before both rate limit hooks: under overload, its 503 preempts the 429 and a shed request spends
 * no permit. Every test uses its own client keys, since the rate limit clock never moves.
 */
public class LoadSheddingInteractionTest {

    private static final String PREFIX = "quarkus.http.rate-limit.rules.all.";

    // one client can hold LIMIT requests open without exhausting its key
    @RegisterExtension
    static final QuarkusExtensionTest config = LoadSheddingInteractionApp.create()
            .overrideConfigKey(PREFIX + "paths", "/*")
            .overrideConfigKey(PREFIX + "phase", "pre-authentication")
            .overrideConfigKey(PREFIX + "request-key", "header:X-Client")
            .overrideRuntimeConfigKey(PREFIX + "permits", "10")
            .overrideRuntimeConfigKey(PREFIX + "window", "1m");

    @Inject
    Vertx vertx;

    @TestHTTPResource
    URL url;

    WebClient client;

    TestClient http;

    @BeforeEach
    void createClient() {
        client = LoadSheddingInteractionApp.client(vertx);
        http = new TestClient(vertx, url);
    }

    @AfterEach
    void releaseAndClose() {
        LoadSheddingInteractionApp.releaseHolds();
        // the next test must not start overloaded
        awaitNotOverloaded();
        client.close();
        http.close();
    }

    @Test
    void overloadAnswers503BeforeTheLimiterAnswers429() throws Exception {
        List<CompletableFuture<HttpResponse<Buffer>>> holds = LoadSheddingInteractionApp.overload(vertx, client, url,
                "a");
        // the 11th would have been a 429, had the limiter seen these requests
        for (int i = 0; i < 11; i++) {
            assertThat(status(vertx, client, url, "/answer", "b")).as("request %d", i).isEqualTo(503);
        }

        LoadSheddingInteractionApp.releaseHolds();
        for (CompletableFuture<HttpResponse<Buffer>> hold : holds) {
            assertThat(hold.get(10, TimeUnit.SECONDS).statusCode()).isEqualTo(200);
        }
        awaitNotOverloaded();

        // nothing was consumed while the requests were shed
        for (int i = 0; i < 10; i++) {
            assertThat(status(vertx, client, url, "/answer", "b")).as("request %d", i).isEqualTo(200);
        }
        assertThat(status(vertx, client, url, "/answer", "b")).isEqualTo(429);
    }

    @Test
    void healthAndMetricsBypassBoth() throws Exception {
        for (int i = 0; i < 10; i++) {
            assertThat(status(vertx, client, url, "/answer", "h")).isEqualTo(200);
        }
        assertThat(status(vertx, client, url, "/answer", "h")).isEqualTo(429);

        LoadSheddingInteractionApp.overload(vertx, client, url, "h-hold");
        assertThat(status(vertx, client, url, "/answer", "h")).isEqualTo(503);
        assertThat(status(vertx, client, url, "/q/health/live", "h")).isEqualTo(200);
        assertThat(status(vertx, client, url, "/q/metrics", "h")).isEqualTo(200);
    }

    @Test
    void bothRetryAfterHeadersAreIntegersAtLeastOne() throws Exception {
        LoadSheddingInteractionApp.overload(vertx, client, url, "r-hold");
        HttpResponse<Buffer> shed = LoadSheddingInteractionApp.get(vertx, client, url, "/answer",
                Map.of("X-Client", "r"));
        assertThat(shed.statusCode()).isEqualTo(503);
        assertRetryAfter(shed);
        LoadSheddingInteractionApp.releaseHolds();
        awaitNotOverloaded();

        for (int i = 0; i < 10; i++) {
            assertThat(status(vertx, client, url, "/answer", "r")).isEqualTo(200);
        }
        HttpResponse<Buffer> limited = LoadSheddingInteractionApp.get(vertx, client, url, "/answer",
                Map.of("X-Client", "r"));
        assertThat(limited.statusCode()).isEqualTo(429);
        assertRetryAfter(limited);
    }

    @Test
    void bothMeterSetsRegister() {
        assertThat(status(vertx, client, url, "/answer", "m")).isEqualTo(200);
        assertScrape(http).hasMetric("http_rate_limit_requests_total")
                .hasMetric(LoadSheddingInteractionApp.SHED_REQUESTS);
    }

    private static void assertRetryAfter(HttpResponse<Buffer> response) {
        assertThat(response.getHeader("Retry-After")).isNotNull().matches("[0-9]+");
        assertThat(Long.parseLong(response.getHeader("Retry-After"))).isGreaterThanOrEqualTo(1);
    }

    private void awaitNotOverloaded() {
        await().atMost(10, TimeUnit.SECONDS).pollInterval(10, TimeUnit.MILLISECONDS).untilAsserted(
                () -> assertScrape(http).hasMetricWithLabelsAndValue(LoadSheddingInteractionApp.OVERLOAD_STATUS, 0));
    }
}
