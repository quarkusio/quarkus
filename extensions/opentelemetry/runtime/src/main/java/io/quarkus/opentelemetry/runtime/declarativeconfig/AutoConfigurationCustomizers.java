package io.quarkus.opentelemetry.runtime.declarativeconfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.Ordered;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.SdkTracerProviderConfigurer;
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
 * so {@link QuarkusDeclarativeSdk} and the Quarkus {@code ComponentProvider}s it adds can apply them at the points
 * where autoconfigure applied them.
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
 * <p>
 * Customizations that upstream's declarative configuration has no seam for are not captured. The providers that
 * register them are reported by {@link #providersWithoutSeam()}, and the legacy engine initializes the SDK instead.
 * </p>
 */
public final class AutoConfigurationCustomizers implements AutoConfigurationCustomizer {

    private BiFunction<Resource, ConfigProperties, Resource> resourceCustomizer = identity();
    private BiFunction<Sampler, ConfigProperties, Sampler> samplerCustomizer = identity();
    private BiFunction<TextMapPropagator, ConfigProperties, TextMapPropagator> propagatorCustomizer = identity();
    private BiFunction<SpanExporter, ConfigProperties, SpanExporter> spanExporterCustomizer = identity();
    private BiFunction<MetricExporter, ConfigProperties, MetricExporter> metricExporterCustomizer = identity();
    private BiFunction<LogRecordExporter, ConfigProperties, LogRecordExporter> logRecordExporterCustomizer = identity();
    private final List<Supplier<Map<String, String>>> propertiesSuppliers = new ArrayList<>();
    private final List<Function<ConfigProperties, Map<String, String>>> propertiesCustomizers = new ArrayList<>();
    private final Set<String> providersWithoutSeam = new LinkedHashSet<>();
    private final Set<String> spanExporterCustomizerProviders = new LinkedHashSet<>();
    private final List<AutoConfigureListener> listeners = new ArrayList<>();
    private String currentProvider;
    private boolean samplerCustomized;
    private boolean propagatorsCustomized;

    private AutoConfigurationCustomizers() {
    }

    @Override
    public AutoConfigurationCustomizer addPropagatorCustomizer(
            BiFunction<? super TextMapPropagator, ConfigProperties, ? extends TextMapPropagator> propagatorCustomizer) {
        this.propagatorCustomizer = merge(this.propagatorCustomizer, propagatorCustomizer);
        this.propagatorsCustomized = true;
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
        this.samplerCustomized = true;
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSpanExporterCustomizer(
            BiFunction<? super SpanExporter, ConfigProperties, ? extends SpanExporter> exporterCustomizer) {
        this.spanExporterCustomizer = merge(this.spanExporterCustomizer, exporterCustomizer);
        spanExporterCustomizerProviders.add(currentProvider);
        return this;
    }

    @Override
    public AutoConfigurationCustomizer addSpanProcessorCustomizer(
            BiFunction<? super SpanProcessor, ConfigProperties, ? extends SpanProcessor> spanProcessorCustomizer) {
        return withoutSeam();
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
        return withoutSeam();
    }

    @Override
    public AutoConfigurationCustomizer addMeterProviderCustomizer(
            BiFunction<SdkMeterProviderBuilder, ConfigProperties, SdkMeterProviderBuilder> meterProviderCustomizer) {
        return withoutSeam();
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
        return withoutSeam();
    }

    @Override
    public AutoConfigurationCustomizer addLoggerProviderCustomizer(
            BiFunction<SdkLoggerProviderBuilder, ConfigProperties, SdkLoggerProviderBuilder> loggerProviderCustomizer) {
        return withoutSeam();
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
        return withoutSeam();
    }

    /**
     * Runs every discovered {@link AutoConfigurationCustomizerProvider} (in {@code order()} order) against a fresh
     * instance and returns it. Every discovered {@link SdkTracerProviderConfigurer} is recorded in
     * {@link #providersWithoutSeam()}, as it customizes the tracer provider builder.
     */
    @SuppressWarnings("deprecation")
    public static AutoConfigurationCustomizers load(ComponentLoader componentLoader) {
        List<AutoConfigurationCustomizerProvider> providers = Ordered.loadOrderedList(componentLoader,
                AutoConfigurationCustomizerProvider.class);
        AutoConfigurationCustomizers customizers = new AutoConfigurationCustomizers();
        for (SdkTracerProviderConfigurer configurer : componentLoader.load(SdkTracerProviderConfigurer.class)) {
            customizers.providersWithoutSeam.add(configurer.getClass().getName());
        }
        for (AutoConfigurationCustomizerProvider provider : providers) {
            if (provider instanceof AutoConfigureListener listener) {
                customizers.listeners.add(listener);
            }
            customizers.currentProvider = provider.getClass().getName();
            provider.customize(customizers);
        }
        customizers.currentProvider = null;
        return customizers;
    }

    /**
     * Applies the registered properties suppliers and customizers to the merged {@code otel.*} map, mirroring
     * autoconfigure: each supplier overrides the map and the suppliers before it, as autoconfigure merged later
     * suppliers over earlier ones and the Quarkus map was registered first, bare {@code otel.*} system properties
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
            effective.putAll(supplier.get());
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
     * Returns the class names of the providers that registered a customization with no declarative configuration
     * equivalent: a tracer, meter or logger provider builder customizer, a span processor, metric reader or log
     * record processor customizer, or an {@link SdkTracerProviderConfigurer}. Only the legacy autoconfigure engine
     * can apply those, so a non-empty result selects it.
     *
     * @return the class names in discovery order, or an empty set
     */
    public Set<String> providersWithoutSeam() {
        return Collections.unmodifiableSet(providersWithoutSeam);
    }

    /**
     * Returns the loaded {@link AutoConfigurationCustomizerProvider} instances that implement
     * {@link AutoConfigureListener}, which autoconfigure calls once the SDK is built.
     *
     * @return the listeners in provider order, or an empty list
     */
    public List<AutoConfigureListener> listeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * An instance with no registrations: every {@code customize*} method is the identity.
     */
    static AutoConfigurationCustomizers none() {
        return new AutoConfigurationCustomizers();
    }

    // The class names of the providers that registered a span exporter customizer, in discovery order
    Set<String> spanExporterCustomizerProviders() {
        return Collections.unmodifiableSet(spanExporterCustomizerProviders);
    }

    // Whether a provider registered a propagator customizer
    boolean customizesPropagators() {
        return propagatorsCustomized;
    }

    // Whether a provider registered a sampler customizer
    boolean customizesSampler() {
        return samplerCustomized;
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

    MetricExporter customizeMetricExporter(MetricExporter exporter, ConfigProperties config) {
        return metricExporterCustomizer.apply(exporter, config);
    }

    LogRecordExporter customizeLogRecordExporter(LogRecordExporter exporter, ConfigProperties config) {
        return logRecordExporterCustomizer.apply(exporter, config);
    }

    // Records the provider currently running; the customization itself is discarded, since the legacy engine,
    // which this selects, loads and runs the provider again.
    private AutoConfigurationCustomizer withoutSeam() {
        providersWithoutSeam.add(currentProvider);
        return this;
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
