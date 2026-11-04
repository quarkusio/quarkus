package io.quarkus.http.ratelimit;

import static io.quarkus.http.ratelimit.MetricsScrape.assertScrape;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import java.net.URL;
import java.time.Duration;

import jakarta.inject.Inject;

import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.http.ratelimit.store.SwitchableTestStore;
import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.http.TestHTTPResource;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

/**
 * {@code max-overdraft=3} over a store that answers in 2 s: the first three requests never wait for the claim,
 * and their admissions are settled by the claims that follow.
 */
public class OptimisticLeaseModeTest {

    private static final String OVERDRAFT = "http_rate_limit_lease_overdraft_total";
    private static final MapEntry<String, String> API = entry("rule", "api");

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp
            .leaseRule(SwitchableStoreApp.create("local", MetricsScrape.class))
            .overrideRuntimeConfigKey(SwitchableStoreApp.STORE + "timeout", "5s")
            .overrideRuntimeConfigKey(SwitchableStoreApp.RULE + "max-overdraft", "3");

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

    @BeforeEach
    void reset() {
        SwitchableTestStore.reset();
    }

    @Test
    void overdraftNeverWaitsAndIsSettled() {
        Duration delay = Duration.ofSeconds(2);
        SwitchableTestStore.behaviour.set(new SwitchableTestStore.Delay(delay));
        long start = System.nanoTime();
        for (int i = 1; i <= 3; i++) {
            HttpResponse<Buffer> response = http.get("/x");
            assertThat(response.statusCode()).as("request %d", i).isEqualTo(200);
        }
        // all three were answered while the only claim (chunk part 1 plus the first admission) is still in
        // flight: the store has seen one call and nothing has been settled yet
        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(delay);
        assertThat(SwitchableTestStore.costs).containsExactly(2L);
        assertScrape(http).hasMetricWithLabelsAndValue(OVERDRAFT, 3, API, entry("result", "admitted"))
                .hasMetricWithLabelsAndValue(OVERDRAFT, 0, API, entry("result", "settled"));

        // the fourth waits for that claim and is served from its chunk; the other two ride on the next claim
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertScrape(http).hasMetricWithLabelsAndValue(OVERDRAFT, 1, API, entry("result", "settled"));
        assertThat(http.get("/x").statusCode()).isEqualTo(200);
        assertThat(SwitchableTestStore.costs).containsExactly(2L, 4L);
        assertScrape(http).hasMetricWithLabelsAndValue(OVERDRAFT, 3, API, entry("result", "settled"))
                .hasMetricWithLabelsAndValue(OVERDRAFT, 0, API, entry("result", "forgiven"));
    }
}
