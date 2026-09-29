package io.quarkus.opentelemetry.runtime.devui;

import java.time.Duration;
import java.util.function.BiFunction;

import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import org.eclipse.microprofile.config.ConfigProvider;

import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.quarkus.dev.telemetry.TelemetryEvent;
import io.quarkus.dev.telemetry.TelemetryEvents;
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

    @Inject
    Event<TelemetryEvent> telemetry;

    @Override
    public void customize(AutoConfiguredOpenTelemetrySdkBuilder builder) {
        // The interval the Dev UI dashboard samples at, read from config: this extension does not depend on Dev UI.
        Duration interval = TelemetryEvents.metricsSampleInterval(ConfigProvider.getConfig()::getOptionalValue);
        builder.addMeterProviderCustomizer(
                new BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder>() {
                    @Override
                    public SdkMeterProviderBuilder apply(SdkMeterProviderBuilder mpBuilder, ConfigProperties cfg) {
                        mpBuilder.registerMetricReader(PeriodicMetricReader.builder(new DevUiMetricsExporter(telemetry))
                                .setInterval(interval)
                                .build());
                        return mpBuilder;
                    }
                });
    }
}
