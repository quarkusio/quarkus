package io.quarkus.http.ratelimit;

import static io.quarkus.test.micrometer.PrometheusMetricsAssert.assertMetrics;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;

import io.quarkus.test.micrometer.PrometheusMetricsAssert;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.client.HttpResponse;

/**
 * Assertions on the Prometheus scrape of {@code /q/metrics}.
 */
final class MetricsScrape {

    private MetricsScrape() {
    }

    static PrometheusMetricsAssert assertScrape(TestClient http) {
        HttpResponse<Buffer> response = http.get("/q/metrics");
        assertThat(response.statusCode()).isEqualTo(200);
        return assertMetrics(new ByteArrayInputStream(response.body().getBytes()));
    }
}
