package io.quarkus.devui.runtime.observability.telemetry;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.dev.telemetry.TelemetryAttributes;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetrySignals;
import io.quarkus.devui.observability.store.metrics.MetricDistribution;
import io.quarkus.devui.observability.store.metrics.MetricSample;

public class MetricEventsTest {

    @Test
    public void aReadingComesBackAsTheSampleItWasBuiltFrom() {
        TelemetryEvent event = TelemetryEvent.metric("queue.depth")
                .value(7).type("gauge").unit("items").source("micrometer")
                .tags(Map.of("region", "eu")).timestamp(1234L)
                .build();

        MetricSample sample = MetricEvents.toSample(event);

        assertThat(sample.name()).isEqualTo("queue.depth");
        assertThat(sample.value()).isEqualTo(7.0);
        assertThat(sample.timestampMillis()).isEqualTo(1234L);
        assertThat(sample.type()).isEqualTo("GAUGE");
        assertThat(sample.cumulative()).isFalse();
        assertThat(sample.unit()).isEqualTo("items");
        assertThat(sample.source()).isEqualTo("micrometer");
        assertThat(sample.tags()).containsEntry("region", "eu");
        assertThat(sample.distribution()).isNull();
    }

    @Test
    public void aDistributionComesBackWithItsPercentilesAndBuckets() {
        TelemetryEvent event = TelemetryEvent.metric("http.server.requests")
                .value(3).type("TIMER").cumulative(true).unit("s")
                .distribution(0.9, 0.5)
                .percentiles(new double[] { 0.5, 0.99 }, new double[] { 0.2, 0.5 })
                .buckets(new double[] { 0.1, 1 }, new double[] { 1, 2, 0 })
                .build();

        MetricDistribution d = MetricEvents.toSample(event).distribution();

        assertThat(d.total()).isEqualTo(0.9);
        assertThat(d.max()).isEqualTo(0.5);
        assertThat(d.hasPercentiles()).isTrue();
        assertThat(d.percentileRanks()).containsExactly(0.5, 0.99);
        assertThat(d.percentileValues()).containsExactly(0.2, 0.5);
        assertThat(d.hasBuckets()).isTrue();
        assertThat(d.bucketBoundaries()).containsExactly(0.1, 1.0);
        assertThat(d.bucketCounts()).containsExactly(1.0, 2.0, 0.0);
    }

    @Test
    public void aDistributionWithoutAMaximumKeepsItUnknown() {
        // A function timer tracks totals only.
        TelemetryEvent event = TelemetryEvent.metric("jobs").value(4).type("TIMER")
                .distribution(12.5, Double.NaN).build();

        MetricDistribution d = MetricEvents.toSample(event).distribution();

        assertThat(d.total()).isEqualTo(12.5);
        assertThat(d.max()).isNaN();
        assertThat(d.hasPercentiles()).isFalse();
        assertThat(d.hasBuckets()).isFalse();
    }

    @Test
    public void aHandWrittenMetricWithDefaultsIsAGaugeFromAnEvent() {
        MetricSample sample = MetricEvents.toSample(new TelemetryEvent(TelemetrySignals.METRIC, "m", 1L,
                Map.of(TelemetryAttributes.VALUE, 1, TelemetryAttributes.TAGS, Map.of("shard", 3))));

        assertThat(sample.type()).isEqualTo("GAUGE");
        assertThat(sample.source()).isEqualTo("event");
        // Tag values are strings on the dashboard, whatever the sender put in.
        assertThat(sample.tags()).containsEntry("shard", "3");
    }

    @Test
    public void aReadingThatWasNotFiniteIsNotASample() {
        assertThat(MetricEvents.toSample(TelemetryEvent.metric("gauge").value(Double.NaN).build())).isNull();
        assertThat(MetricEvents.toSample(new TelemetryEvent(TelemetrySignals.METRIC, "m", 1L,
                Map.of(TelemetryAttributes.VALUE, "seven")))).isNull();
    }
}
