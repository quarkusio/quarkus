package io.quarkus.devui.observability.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MetricCatalogTest {

    @Test
    void groupsByNamePrefixAndCountsSeries() {
        MetricCatalog c = new MetricCatalog();
        c.record(new MetricSample("http.server.requests", Map.of("uri", "/a"), "TIMER", true, 1, 10L, "micrometer"));
        c.record(new MetricSample("http.server.requests", Map.of("uri", "/b"), "TIMER", true, 1, 10L, "micrometer"));
        c.record(new MetricSample("jvm.memory.used", Map.of("area", "heap"), "GAUGE", false, 5, 10L, "micrometer"));

        List<MetricCatalogEntry> entries = c.snapshot();
        // Two distinct metric names, grouped by name prefix.
        assertThat(entries).hasSize(2);
        assertThat(entries).extracting(MetricCatalogEntry::group).contains("http", "jvm");

        MetricCatalogEntry http = entries.stream()
                .filter(e -> e.name().equals("http.server.requests")).findFirst().orElseThrow();
        assertThat(http.group()).isEqualTo("http");
        assertThat(http.seriesCount()).isEqualTo(2);
        assertThat(http.cumulative()).isTrue();
    }

    @Test
    void nameWithoutDotIsItsOwnGroup() {
        MetricCatalog c = new MetricCatalog();
        c.record(new MetricSample("uptime", Map.of(), "GAUGE", false, 1, 10L, "otel"));
        assertThat(c.snapshot().get(0).group()).isEqualTo("uptime");
    }
}
