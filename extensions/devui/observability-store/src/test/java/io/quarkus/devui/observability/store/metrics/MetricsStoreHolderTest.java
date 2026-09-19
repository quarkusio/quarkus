package io.quarkus.devui.observability.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetricsStoreHolderTest {

    @Test
    void sameConfigReturnsSameInstanceDifferentConfigRebuilds() {
        MetricsTimeSeriesStore a = MetricsStoreHolder.getOrCreate(600_000L, 720, 5_000L);
        MetricsTimeSeriesStore b = MetricsStoreHolder.getOrCreate(600_000L, 720, 5_000L);
        assertThat(b).isSameAs(a); // reused across a "reload"

        MetricsTimeSeriesStore c = MetricsStoreHolder.getOrCreate(1_000L, 720, 5_000L);
        assertThat(c).isNotSameAs(a); // config changed -> rebuilt
    }
}
