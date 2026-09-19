package io.quarkus.devui.observability.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MetricSampleTest {

    @Test
    void seriesKeyIsStableRegardlessOfTagOrder() {
        Map<String, String> a = new LinkedHashMap<>();
        a.put("method", "GET");
        a.put("status", "200");
        Map<String, String> b = new LinkedHashMap<>();
        b.put("status", "200");
        b.put("method", "GET");

        MetricSample s1 = new MetricSample("http.server.requests", a, "TIMER", true, 3, 1000L, "micrometer");
        MetricSample s2 = new MetricSample("http.server.requests", b, "TIMER", true, 4, 2000L, "micrometer");

        assertThat(s1.seriesKey()).isEqualTo(s2.seriesKey());
        assertThat(s1.seriesKey()).isEqualTo("micrometer|http.server.requests{method=GET,status=200}");
    }

    @Test
    void sameNameAndTagsFromDifferentSourcesAreDistinctSeries() {
        MetricSample mm = new MetricSample("m", java.util.Map.of("k", "v"), "COUNTER", true, 1, 10L, "micrometer");
        MetricSample ot = new MetricSample("m", java.util.Map.of("k", "v"), "COUNTER", true, 1, 10L, "otel");
        assertThat(mm.seriesKey()).isNotEqualTo(ot.seriesKey());
    }
}
