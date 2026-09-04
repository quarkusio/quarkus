package io.quarkus.opentelemetry.runtime.devui;

import java.time.Duration;
import java.util.function.BiFunction;

import jakarta.inject.Inject;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.quarkus.devui.observability.store.metrics.MetricsTimeSeriesStore;
import io.quarkus.opentelemetry.runtime.AutoConfiguredOpenTelemetrySdkBuilderCustomizer;

/**
 * Dev-mode-only: registers an additional in-memory PeriodicMetricReader on the SDK meter
 * provider so native (and bridged) OTel metrics are captured for the Dev UI, alongside the
 * normal OTLP export pipeline.
 *
 * NOTE: NO class-level scope annotation — registered as a bean only by the dev-only build
 * step (which supplies {@code @Singleton}).
 */
public class DevUiMetricsSdkBuilderCustomizer implements AutoConfiguredOpenTelemetrySdkBuilderCustomizer {

    // Produced by MetricsStoreProducer in the quarkus-devui runtime; the sampling interval is
    // carried on the store, so this customizer needs no config dependency.
    @Inject
    MetricsTimeSeriesStore store;

    @Override
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
        Duration interval = Duration.ofMillis(store.sampleIntervalMillis());
        builder.addMeterProviderCustomizer(
                new BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder>() {
                    @Override
                    public SdkMeterProviderBuilder apply(SdkMeterProviderBuilder mpBuilder, ConfigProperties cfg) {
                        mpBuilder.registerMetricReader(PeriodicMetricReader.builder(new DevUiMetricsExporter(store))
                                .setInterval(interval)
                                .build());
                        return mpBuilder;
                    }
                });
    }
}
