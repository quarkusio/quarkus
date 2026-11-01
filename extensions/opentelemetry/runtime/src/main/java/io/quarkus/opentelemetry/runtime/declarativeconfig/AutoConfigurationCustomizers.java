package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.SdkLoggerProviderBuilder;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.SdkMeterProviderBuilder;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * Captures the functions that {@link AutoConfigurationCustomizerProvider} implementations (a ServiceLoader SPI that
 * MicroProfile Telemetry requires to be honored) register against the {@link AutoConfigurationCustomizer} interface,
 * so {@code OpenTelemetryRecorder} and {@link QuarkusDeclarativeConfiguration} can apply them at the points where
 * autoconfigure applied them.
 * <p>
 * This is a transitional compatibility bridge. Upstream's declarative configuration deliberately does not run the
 * autoconfigure customization SPIs (its replacements are {@code ComponentProvider} and
 * {@code DeclarativeConfigurationCustomizerProvider}). It exists for compatibility with the autoconfigure ecosystem
 * and MicroProfile Telemetry compliance; should a future MicroProfile Telemetry release stop requiring the
 * autoconfigure SPIs, this class can be deleted.
 * </p>
 * <p>
 * Ordering matches autoconfigure's effective composition: it invoked the ServiceLoader-provided customizers at
 * build time, after the Quarkus customizer beans had registered, so the captured SPI functions always run last,
 * over the Quarkus-wired component.
 * </p>
 */
public final class AutoConfigurationCustomizers implements AutoConfigurationCustomizer {

    private BiFunction<Resource, ConfigProperties, Resource> resourceCustomizer = identity();
    private BiFunction<Sampler, ConfigProperties, Sampler> samplerCustomizer = identity();
    private BiFunction<TextMapPropagator, ConfigProperties, TextMapPropagator> propagatorCustomizer = identity();
    private BiFunction<SpanExporter, ConfigProperties, SpanExporter> spanExporterCustomizer = identity();
    private BiFunction<SpanProcessor, ConfigProperties, SpanProcessor> spanProcessorCustomizer = identity();
    private BiFunction<MetricExporter, ConfigProperties, MetricExporter> metricExporterCustomizer = identity();
    private BiFunction<MetricReader, ConfigProperties, MetricReader> metricReaderCustomizer = identity();
    private BiFunction<LogRecordExporter, ConfigProperties, LogRecordExporter> logRecordExporterCustomizer = identity();
    private BiFunction<LogRecordProcessor, ConfigProperties, LogRecordProcessor> logRecordProcessorCustomizer = identity();
    private BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> tracerProviderCustomizer = identity();
    private BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> meterProviderCustomizer = identity();
    private BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder> loggerProviderCustomizer = identity();
    private final List<Supplier<Map<String, String>>> propertiesSuppliers = new ArrayList<>();
    private final List<Function<ConfigProperties, Map<String, String>>> propertiesCustomizers = new ArrayList<>();

    private AutoConfigurationCustomizers() {
    }

    @Override
    public AutoConfigurationCustomizer addPropagatorCustomizer(
            BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator> propagatorCustomizer) {
        this.propagatorCustomizer = merge(this.propagatorCustomizer, propagatorCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addResourceCustomizer(
            BiFunction<? super Resource, ConfigProperties, ? extends Resource> resourceCustomizer) {
        this.resourceCustomizer = merge(this.resourceCustomizer, resourceCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSamplerCustomizer(
            BiFunction<? super Sampler, ConfigProperties, ? extends Sampler> samplerCustomizer) {
        this.samplerCustomizer = merge(this.samplerCustomizer, samplerCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSpanExporterCustomizer(
            BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter> exporterCustomizer) {
        this.spanExporterCustomizer = merge(this.spanExporterCustomizer, exporterCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSpanProcessorCustomizer(
            BiFunction<? super SpanProcessor, ConfigProperties, ? extends SpanProcessor> spanProcessorCustomizer) {
        this.spanProcessorCustomizer = merge(this.spanProcessorCustomizer, spanProcessorCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesSupplier(Supplier<Map<String, String>> propertiesSupplier) {
        this.propertiesSuppliers.add(propertiesSupplier);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addPropertiesCustomizer(
            Function<ConfigProperties, Map<String, String>> propertiesCustomizer) {
        this.propertiesCustomizers.add(propertiesCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addTracerProviderCustomizer(
            BiFunction<SdkTracerProviderBuilder, ConfigProperties, SdkTracerProviderBuilder> tracerProviderCustomizer) {
        this.tracerProviderCustomizer = merge(this.tracerProviderCustomizer, tracerProviderCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addMeterProviderCustomizer(
            BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> meterProviderCustomizer) {
        this.meterProviderCustomizer = merge(this.meterProviderCustomizer, meterProviderCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addMetricExporterCustomizer(
            BiFunction<? super MetricExporter, ConfigProperties, ? extends MetricExporter> exporterCustomizer) {
        this.metricExporterCustomizer = merge(this.metricExporterCustomizer, exporterCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addMetricReaderCustomizer(
            BiFunction<? super MetricReader, ConfigProperties, ? extends MetricReader> readerCustomizer) {
        this.metricReaderCustomizer = merge(this.metricReaderCustomizer, readerCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addLoggerProviderCustomizer(
            BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder> loggerProviderCustomizer) {
        this.loggerProviderCustomizer = merge(this.loggerProviderCustomizer, loggerProviderCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addLogRecordExporterCustomizer(
            BiFunction<? super LogRecordExporter, ConfigProperties, ? extends LogRecordExporter> exporterCustomizer) {
        this.logRecordExporterCustomizer = merge(this.logRecordExporterCustomizer, exporterCustomizer);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addLogRecordProcessorCustomizer(
            BiFunction<? super LogRecordProcessor, ConfigProperties, ? extends LogRecordProcessor> logRecordProcessorCustomizer) {
        this.logRecordProcessorCustomizer = merge(this.logRecordProcessorCustomizer, logRecordProcessorCustomizer);
        return this;
    }

    /**
     * Runs every discovered {@link AutoConfigurationCustomizerProvider} (in {@code order()} order) against a fresh
     * instance and returns it.
     */
    public static AutoConfigurationCustomizers load(ComponentLoader componentLoader) {
        List<AutoConfigurationCustomizerProvider> providers = new ArrayList<>();
        componentLoader.load(AutoConfigurationCustomizerProvider.class).forEach(providers::add);
        providers.sort(Comparator.comparingInt(AutoConfigurationCustomizerProvider::order));
        AutoConfigurationCustomizers customizers = new AutoConfigurationCustomizers();
        for (AutoConfigurationCustomizerProvider provider : providers) {
            provider.customize(customizers);
        }
        return customizers;
    }

    /**
     * Applies the registered properties suppliers and customizers to the merged {@code otel.*} map, mirroring
     * autoconfigure: supplied properties are defaults (existing values win), bare {@code otel.*} system properties
     * and {@code OTEL_*} environment variables override them (system properties over environment variables, both
     * over the map, as {@link DefaultConfigProperties#create} layers them and the legacy
     * {@code AutoConfiguredOpenTelemetrySdk} path did), each properties customizer sees the result of the previous
     * one, and the entries it returns override everything. The result is the {@link ConfigProperties} view
     * everything else works from, so properties customizers influence model decisions the way they influenced
     * autoconfigure's.
     */
    public ConfigProperties resolveConfigProperties(Map<String, String> otelConfigs, ComponentLoader componentLoader) {
        Map<String, String> effective = new HashMap<>(otelConfigs);
        for (Supplier<Map<String, String>> supplier : propertiesSuppliers) {
            supplier.get().forEach(effective::putIfAbsent);
        }
        DefaultConfigProperties properties = DefaultConfigProperties.create(effective, componentLoader);
        for (Function<ConfigProperties, Map<String, String>> customizer : propertiesCustomizers) {
            Map<String, String> overrides = customizer.apply(properties);
            if (overrides != null && !overrides.isEmpty()) {
                properties = properties.withOverrides(overrides);
            }
        }
        return properties;
    }

    public Resource customizeResource(Resource resource, ConfigProperties config) {
        return resourceCustomizer.apply(resource, config);
    }

    /**
     * An instance with no registrations: every {@code customize*} method is the identity.
     */
    static AutoConfigurationCustomizers none() {
        return new AutoConfigurationCustomizers();
    }

    Sampler customizeSampler(Sampler sampler, ConfigProperties config) {
        return samplerCustomizer.apply(sampler, config);
    }

    TextMapPropagator customizePropagator(TextMapPropagator propagator, ConfigProperties config) {
        return propagatorCustomizer.apply(propagator, config);
    }

    SpanExporter customizeSpanExporter(SpanExporter exporter, ConfigProperties config) {
        return spanExporterCustomizer.apply(exporter, config);
    }

    SpanProcessor customizeSpanProcessor(SpanProcessor processor, ConfigProperties config) {
        return spanProcessorCustomizer.apply(processor, config);
    }

    MetricExporter customizeMetricExporter(MetricExporter exporter, ConfigProperties config) {
        return metricExporterCustomizer.apply(exporter, config);
    }

    MetricReader customizeMetricReader(MetricReader reader, ConfigProperties config) {
        return metricReaderCustomizer.apply(reader, config);
    }

    LogRecordExporter customizeLogRecordExporter(LogRecordExporter exporter, ConfigProperties config) {
        return logRecordExporterCustomizer.apply(exporter, config);
    }

    LogRecordProcessor customizeLogRecordProcessor(LogRecordProcessor processor, ConfigProperties config) {
        return logRecordProcessorCustomizer.apply(processor, config);
    }

    SdkTracerProviderBuilder customizeTracerProviderBuilder(SdkTracerProviderBuilder builder,
            ConfigProperties config) {
        return tracerProviderCustomizer.apply(builder, config);
    }

    SdkMeterProviderBuilder customizeMeterProviderBuilder(SdkMeterProviderBuilder builder,
            ConfigProperties config) {
        return meterProviderCustomizer.apply(builder, config);
    }

    SdkLoggerProviderBuilder customizeLoggerProviderBuilder(SdkLoggerProviderBuilder builder,
            ConfigProperties config) {
        return loggerProviderCustomizer.apply(builder, config);
    }

    private static <T> BiFunction<T, ConfigProperties, T> identity() {
        return (value, config) -> value;
    }

    // Composes registrations the way autoconfigure's mergeCustomizer does: later registrations see the result of
    // earlier ones.
    private static <T> BiFunction<T, ConfigProperties, T> merge(BiFunction<T, ConfigProperties, T> first,
            BiFunction<? super T, ConfigProperties, ? extends T> second) {
        return (value, config) -> second.apply(first.apply(value, config), config);
    }
}
