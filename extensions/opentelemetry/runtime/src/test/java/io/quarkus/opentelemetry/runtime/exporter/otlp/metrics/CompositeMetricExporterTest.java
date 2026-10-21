package io.quarkus.opentelemetry.runtime.exporter.otlp.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;

class CompositeMetricExporterTest {

    @Test
    void ofEmptyReturnsNoop() {
        assertThat(CompositeMetricExporter.of(List.of())).isSameAs(NoopMetricExporter.INSTANCE);
    }

    @Test
    void ofSingleReturnsSameInstance() {
        MetricExporter single = new RecordingMetricExporter(AggregationTemporality.CUMULATIVE);
        assertThat(CompositeMetricExporter.of(List.of(single))).isSameAs(single);
    }

    @Test
    void fansOutToAllDelegates() {
        RecordingMetricExporter first = new RecordingMetricExporter(AggregationTemporality.CUMULATIVE);
        RecordingMetricExporter second = new RecordingMetricExporter(AggregationTemporality.DELTA);

        MetricExporter composite = CompositeMetricExporter.of(List.of(first, second));
        assertThat(composite).isInstanceOf(CompositeMetricExporter.class);

        assertThat(composite.export(List.of()).isSuccess()).isTrue();
        assertThat(composite.flush().isSuccess()).isTrue();
        assertThat(composite.shutdown().isSuccess()).isTrue();

        assertThat(first.exportCount.get()).isEqualTo(1);
        assertThat(second.exportCount.get()).isEqualTo(1);
        assertThat(first.flushCount.get()).isEqualTo(1);
        assertThat(second.flushCount.get()).isEqualTo(1);
        assertThat(first.shutdownCount.get()).isEqualTo(1);
        assertThat(second.shutdownCount.get()).isEqualTo(1);

        // temporality follows the first delegate
        assertThat(composite.getAggregationTemporality(InstrumentType.COUNTER))
                .isEqualTo(AggregationTemporality.CUMULATIVE);
    }

    @Test
    void oneThrowingDelegateDoesNotStopTheOthers() {
        RecordingMetricExporter before = new RecordingMetricExporter(AggregationTemporality.CUMULATIVE);
        ThrowingMetricExporter throwing = new ThrowingMetricExporter();
        RecordingMetricExporter after = new RecordingMetricExporter(AggregationTemporality.CUMULATIVE);

        MetricExporter composite = CompositeMetricExporter.of(List.of(before, throwing, after));

        // The failure of one delegate is folded into the aggregated result...
        assertThat(composite.export(List.of()).isSuccess()).isFalse();
        assertThat(composite.flush().isSuccess()).isFalse();
        assertThat(composite.shutdown().isSuccess()).isFalse();

        // ...but the delegates surrounding the failing one still run.
        assertThat(before.exportCount.get()).isEqualTo(1);
        assertThat(after.exportCount.get()).isEqualTo(1);
        assertThat(before.flushCount.get()).isEqualTo(1);
        assertThat(after.flushCount.get()).isEqualTo(1);
        assertThat(before.shutdownCount.get()).isEqualTo(1);
        assertThat(after.shutdownCount.get()).isEqualTo(1);
    }

    private static final class ThrowingMetricExporter implements MetricExporter {
        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            throw new RuntimeException("boom on export");
        }

        @Override
        public CompletableResultCode flush() {
            throw new RuntimeException("boom on flush");
        }

        @Override
        public CompletableResultCode shutdown() {
            throw new RuntimeException("boom on shutdown");
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return AggregationTemporality.CUMULATIVE;
        }
    }

    private static final class RecordingMetricExporter implements MetricExporter {
        final AtomicInteger exportCount = new AtomicInteger();
        final AtomicInteger flushCount = new AtomicInteger();
        final AtomicInteger shutdownCount = new AtomicInteger();
        final AggregationTemporality temporality;

        RecordingMetricExporter(AggregationTemporality temporality) {
            this.temporality = temporality;
        }

        @Override
        public CompletableResultCode export(Collection<MetricData> metrics) {
            exportCount.incrementAndGet();
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode flush() {
            flushCount.incrementAndGet();
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public CompletableResultCode shutdown() {
            shutdownCount.incrementAndGet();
            return CompletableResultCode.ofSuccess();
        }

        @Override
        public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
            return temporality;
        }
    }
}
