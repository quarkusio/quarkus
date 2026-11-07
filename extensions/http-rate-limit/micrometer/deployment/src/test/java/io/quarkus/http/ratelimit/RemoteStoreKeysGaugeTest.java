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
 * The tracked-keys gauge exists only for rules whose keys live in this replica's memory.
 */
public class RemoteStoreKeysGaugeTest {

    @RegisterExtension
    static final QuarkusExtensionTest config = SwitchableStoreApp.rule(
            SwitchableStoreApp.rule(SwitchableStoreApp.create("local", MetricsScrape.class), "remote", "/r/*",
                    "switchable"),
            "loc", "/l/*", "local");

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
    void onlyLocalRulesHaveAKeysGauge() {
        assertThat(http.get("/r/x").statusCode()).isEqualTo(200);
        assertThat(http.get("/l/x").statusCode()).isEqualTo(200);

        assertScrape(http)
                .hasMetricWithLabels("http_rate_limit_keys", entry("rule", "loc"))
                .doesNotHaveMetricWithLabels("http_rate_limit_keys", entry("rule", "remote"))
                .hasMetricWithLabelsAndValue("http_rate_limit_requests_total", 1, entry("rule", "remote"),
                        entry("result", "allowed"));
    }
}
