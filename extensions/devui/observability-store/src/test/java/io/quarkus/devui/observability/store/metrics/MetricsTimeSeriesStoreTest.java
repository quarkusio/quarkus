package io.quarkus.devui.observability.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class MetricsTimeSeriesStoreTest {

    private MetricSample sample(String name, String tagVal, double value, long ts) {
        return new MetricSample(name, Map.of("k", tagVal), "GAUGE", false, value, ts, "micrometer");
    }

    // (retentionMillis, maxPointsPerSeries, sampleIntervalMillis)
    private MetricsTimeSeriesStore newStore(long retentionMillis, int maxPoints) {
        return new MetricsTimeSeriesStore(retentionMillis, maxPoints, 5_000L);
    }

    @Test
    void unselectedMetricsUpdateCatalogButStoreNoHistory() {
        MetricsTimeSeriesStore store = newStore(60_000L, 100);
        store.observe(sample("a.x", "1", 1, 1000L));

        // Catalog knows about it...
        assertThat(store.catalog().snapshot()).hasSize(1);
        // ...but nothing is stored (not selected), so the snapshot is empty.
        assertThat(store.snapshot()).isEmpty();
        assertThat(store.seriesCount()).isZero();
    }

    @Test
    void onlySelectedNamesAccumulatePoints() {
        MetricsTimeSeriesStore store = newStore(60_000L, 100);
        store.setSelection(List.of("a.x"));
        store.observe(sample("a.x", "1", 1, 1000L));
        store.observe(sample("a.x", "1", 2, 2000L));
        store.observe(sample("b.y", "1", 9, 2000L)); // not selected

        List<MetricSeriesSnapshot> snap = store.snapshot();
        assertThat(snap).hasSize(1);
        MetricSeriesSnapshot ser = snap.get(0);
        assertThat(ser.name()).isEqualTo("a.x");
        assertThat(ser.timestamps()).containsExactly(1000L, 2000L);
    }

    @Test
    void deselectingDropsStoredSeries() {
        MetricsTimeSeriesStore store = newStore(60_000L, 100);
        store.setSelection(List.of("a.x"));
        store.observe(sample("a.x", "1", 1, 1000L));
        assertThat(store.seriesCount()).isEqualTo(1);

        store.setSelection(List.of()); // deselect everything
        assertThat(store.seriesCount()).isZero();
        assertThat(store.snapshot()).isEmpty();
    }

    @Test
    void retentionEvictsOldPointsOnObserve() {
        MetricsTimeSeriesStore store = newStore(1_000L, 100); // 1s window
        store.setSelection(List.of("a.x"));
        store.observe(sample("a.x", "1", 1, 1_000L));
        store.observe(sample("a.x", "1", 2, 2_500L)); // 1_500ms later -> first point (ts 1000 < 1500) evicted
        MetricSeriesSnapshot ser = store.snapshot().get(0);
        assertThat(ser.timestamps()).containsExactly(2_500L);
    }

    @Test
    void clearResetsHistoryCatalogAndSelection() {
        MetricsTimeSeriesStore store = newStore(60_000L, 100);
        store.setSelection(List.of("a.x"));
        store.observe(sample("a.x", "1", 1, 1000L));
        assertThat(store.seriesCount()).isEqualTo(1);
        assertThat(store.catalog().snapshot()).hasSize(1);

        store.clear();
        // History, catalog and selection are all gone.
        assertThat(store.snapshot()).isEmpty();
        assertThat(store.seriesCount()).isZero();
        assertThat(store.catalog().snapshot()).isEmpty();
        // Selection was cleared, so a further observe of the previously-selected name is not stored.
        store.observe(sample("a.x", "1", 2, 2000L));
        assertThat(store.snapshot()).isEmpty();
    }

    @Test
    void resetCatalogKeepsSelectionAndHistory() {
        MetricsTimeSeriesStore store = newStore(60_000L, 100);
        store.setSelection(List.of("a.x"));
        store.observe(sample("a.x", "1", 1, 1000L));
        assertThat(store.catalog().snapshot()).hasSize(1);
        assertThat(store.seriesCount()).isEqualTo(1);

        // Mirrors the once-per-reload catalog reset done by the producer.
        store.resetCatalog();
        assertThat(store.catalog().snapshot()).isEmpty();
        // History survives...
        assertThat(store.snapshot()).hasSize(1);
        // ...and because the selection is kept, capture resumes into the same series after reload.
        store.observe(sample("a.x", "1", 2, 2000L));
        MetricSeriesSnapshot ser = store.snapshot().get(0);
        assertThat(ser.timestamps()).containsExactly(1000L, 2000L);
        // The catalog repopulates from the next observed sample.
        assertThat(store.catalog().snapshot()).hasSize(1);
    }

    @Test
    void nonFiniteValuesAreIgnored() {
        MetricsTimeSeriesStore store = newStore(60_000L, 100);
        store.setSelection(List.of("a.x"));
        store.observe(new MetricSample("a.x", Map.of("k", "1"), "GAUGE", false, Double.NaN, 1000L, "micrometer"));
        assertThat(store.snapshot()).isEmpty();
    }
}
