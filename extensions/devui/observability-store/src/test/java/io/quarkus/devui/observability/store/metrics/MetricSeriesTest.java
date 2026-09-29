package io.quarkus.devui.observability.store.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MetricSeriesTest {

    private MetricSeries newSeries(int maxPoints) {
        return new MetricSeries("m", Map.of("k", "v"), "GAUGE", false, "micrometer", null, maxPoints);
    }

    private MetricSeries newTimerSeries(int maxPoints) {
        return new MetricSeries("m", Map.of("k", "v"), "TIMER", true, "micrometer", "s", maxPoints);
    }

    @Test
    void ringWrapsAtMaxPointsKeepingNewest() {
        MetricSeries s = newSeries(3);
        for (int i = 1; i <= 5; i++) {
            s.add(i * 10L, i, null);
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
        s.add(100L, 1, null);
        s.add(200L, 2, null);
        s.add(300L, 3, null);
        s.evictOlderThan(250L);
        assertThat(s.size()).isEqualTo(1);
        assertThat(s.snapshot().timestamps()).containsExactly(300L);
    }

    @Test
    void noDistributionColumnsForAPlainSeries() {
        MetricSeries s = newSeries(10);
        s.add(100L, 1, null);
        assertThat(s.snapshot().distribution()).isNull();
    }

    @Test
    void distributionColumnsRideAlongWithThePoints() {
        MetricSeries s = newTimerSeries(10);
        s.add(100L, 2, new MetricDistribution(0.5, 0.3));
        s.add(200L, 5, new MetricDistribution(1.25, 0.4));

        MetricSeriesSnapshot.Distribution d = s.snapshot().distribution();
        assertThat(d).isNotNull();
        assertThat(d.totals()).containsExactly(0.5, 1.25);
        assertThat(d.maxes()).containsExactly(0.3, 0.4);
        assertThat(d.percentileRanks()).isEmpty();
        assertThat(d.bucketBoundaries()).isEmpty();
    }

    @Test
    void distributionColumnsWrapWithTheRing() {
        MetricSeries s = newTimerSeries(2);
        s.add(100L, 1, new MetricDistribution(1, 1));
        s.add(200L, 2, new MetricDistribution(2, 2));
        s.add(300L, 3, new MetricDistribution(3, 3));

        MetricSeriesSnapshot snap = s.snapshot();
        assertThat(snap.timestamps()).containsExactly(200L, 300L);
        assertThat(snap.distribution().totals()).containsExactly(2.0, 3.0);
        assertThat(snap.distribution().maxes()).containsExactly(2.0, 3.0);
    }

    @Test
    void percentilesAreKeptPerRank() {
        MetricSeries s = newTimerSeries(10);
        double[] ranks = { 0.5, 0.95 };
        s.add(100L, 1, new MetricDistribution(1, 1, ranks, new double[] { 0.1, 0.2 }, null, null));
        s.add(200L, 2, new MetricDistribution(2, 2, ranks, new double[] { 0.3, 0.4 }, null, null));

        MetricSeriesSnapshot.Distribution d = s.snapshot().distribution();
        assertThat(d.percentileRanks()).containsExactly(0.5, 0.95);
        assertThat(d.percentileValues()[0]).containsExactly(0.1, 0.3);
        assertThat(d.percentileValues()[1]).containsExactly(0.2, 0.4);
    }

    @Test
    void bucketsBaselineOnFirstCollectionThenAccumulate() {
        MetricSeries s = newTimerSeries(10);
        double[] bounds = { 0.1, 0.5 };
        // First collection is the baseline: it covers traffic from before this meter was charted.
        s.add(100L, 10, buckets(bounds, 7, 2, 1));
        assertThat(s.snapshot().distribution().bucketCounts()).containsExactly(0.0, 0.0, 0.0);

        s.add(200L, 14, buckets(bounds, 10, 3, 1));
        assertThat(s.snapshot().distribution().bucketCounts()).containsExactly(3.0, 1.0, 0.0);

        s.add(300L, 16, buckets(bounds, 11, 3, 2));
        assertThat(s.snapshot().distribution().bucketCounts()).containsExactly(4.0, 1.0, 1.0);
        assertThat(s.snapshot().distribution().bucketBoundaries()).containsExactly(0.1, 0.5);
    }

    @Test
    void aMeterRecreatedByALiveReloadCountsFromZeroRatherThanGoingNegative() {
        MetricSeries s = newTimerSeries(10);
        double[] bounds = { 0.1 };
        s.add(100L, 5, buckets(bounds, 4, 1));
        s.add(200L, 9, buckets(bounds, 7, 2));
        assertThat(s.snapshot().distribution().bucketCounts()).containsExactly(3.0, 1.0);

        // Reload: the meter is new, so its counts restart below the previous baseline.
        s.add(300L, 2, buckets(bounds, 2, 0));
        assertThat(s.snapshot().distribution().bucketCounts()).containsExactly(5.0, 1.0);
    }

    @Test
    void theStreamedBucketCountsMatchTheSnapshot() {
        MetricSeries s = newTimerSeries(10);
        double[] bounds = { 0.1 };
        s.add(100L, 1, buckets(bounds, 1, 0));
        double[] streamed = s.add(200L, 4, buckets(bounds, 3, 1));
        assertThat(streamed).containsExactly(s.snapshot().distribution().bucketCounts());
    }

    private static MetricDistribution buckets(double[] boundaries, double... counts) {
        return new MetricDistribution(0, 0, null, null, boundaries, counts);
    }
}
