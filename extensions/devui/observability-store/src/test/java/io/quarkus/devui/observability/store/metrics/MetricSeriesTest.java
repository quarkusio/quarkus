package io.quarkus.devui.observability.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MetricSeriesTest {

    private MetricSeries newSeries(int maxPoints) {
        return new MetricSeries("m", Map.of("k", "v"), "GAUGE", false, "micrometer", maxPoints);
    }

    @Test
    void ringWrapsAtMaxPointsKeepingNewest() {
        MetricSeries s = newSeries(3);
        for (int i = 1; i <= 5; i++) {
            s.add(i * 10L, i);
        }
        assertThat(s.size()).isEqualTo(3);
        // Oldest two (ts 10,20) evicted; expect ts 30,40,50.
        MetricSeriesSnapshot snap = s.snapshot();
        assertThat(snap.timestamps()).containsExactly(30L, 40L, 50L);
        assertThat(snap.values()).containsExactly(3.0, 4.0, 5.0);
    }

    @Test
    void evictOlderThanDropsFromFront() {
        MetricSeries s = newSeries(10);
        s.add(100L, 1);
        s.add(200L, 2);
        s.add(300L, 3);
        s.evictOlderThan(250L);
        assertThat(s.size()).isEqualTo(1);
        assertThat(s.snapshot().timestamps()).containsExactly(300L);
    }
}
