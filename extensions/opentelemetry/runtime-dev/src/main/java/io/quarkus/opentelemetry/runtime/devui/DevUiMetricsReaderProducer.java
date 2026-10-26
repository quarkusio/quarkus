package io.quarkus.opentelemetry.runtime.devui;

import java.time.Duration;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;

/**
 * Dev-mode-only: produces an additional in-memory {@link MetricReader} bean that
 * {@code OpenTelemetryRecorder} registers on the SDK meter provider so native (and bridged)
 * OTel metrics are captured for the Dev UI, alongside the normal OTLP export pipeline.
 * <p>
 * NOTE: NO class-level scope annotation — registered as a bean only by the dev-only build
 * step (which supplies {@code @Singleton}).
 * </p>
 */
public class DevUiMetricsReaderProducer {

    // The store is produced by MetricsStoreProducer in the quarkus-devui runtime; the sampling
    // interval is carried on the store, so this producer needs no config dependency.
    @Produces
    @Singleton
    MetricReader devUiMetricsReader(MetricsTimeSeriesStore store) {
        return PeriodicMetricReader.builder(new DevUiMetricsExporter(store))
                .setInterval(Duration.ofMillis(store.sampleIntervalMillis()))
                .build();
    }
}
