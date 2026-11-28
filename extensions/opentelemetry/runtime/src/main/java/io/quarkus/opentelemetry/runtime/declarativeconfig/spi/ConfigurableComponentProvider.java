package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jboss.logging.Logger;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigurablePropagatorProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.AutoConfigureListener;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ConfigurableMetricReaderProvider;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.metrics.ConfigurableMetricExporterProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSamplerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;

/**
 * A {@link ComponentProvider} that creates its component through an autoconfigure {@code Configurable*Provider}.
 * The provider receives the flat {@code otel.*} {@link ConfigProperties}, as on the autoconfigure path, and the
 * declarative configuration node passed to {@link #create(DeclarativeConfigProperties)} is not used.
 * <p>
 * Once this adapter has created a component, {@link #afterAutoConfigure(OpenTelemetrySdk)} is forwarded to the
 * wrapped provider and to each component it created that implements {@link AutoConfigureListener}, as autoconfigure
 * calls them. A listener that throws is logged and does not stop the others. An adapter that created no component
 * forwards nothing.
 * </p>
 * <p>
 * Instances are not thread-safe; the SDK is built on one thread.
 * </p>
 *
 * @param <S> the autoconfigure SPI type
 */
public final class ConfigurableComponentProvider<S> implements ComponentProvider, AutoConfigureListener {

    private static final Logger LOG = Logger.getLogger(ConfigurableComponentProvider.class);

    private final Class<?> type;
    private final String name;
    private final S provider;
    private final BiFunction<S, ConfigProperties, ?> factory;
    private final ConfigProperties config;
    private final List<Object> components = new ArrayList<>();

    private ConfigurableComponentProvider(Class<?> type, String name, S provider,
            BiFunction<S, ConfigProperties, ?> factory, ConfigProperties config) {
        this.type = type;
        this.name = name;
        this.provider = provider;
        this.factory = factory;
        this.config = config;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object create(DeclarativeConfigProperties ignored) {
        Object component = factory.apply(provider, config);
        if (component != null) {
            components.add(component);
        }
        return component;
    }

    @Override
    public void afterAutoConfigure(OpenTelemetrySdk sdk) {
        if (components.isEmpty()) {
            return;
        }
        // Each instance once, as autoconfigure's SpiHelper keeps an identity set: a provider can return itself
        // or the same component every time
        Set<Object> targets = Collections.newSetFromMap(new IdentityHashMap<>());
        targets.add(provider);
        targets.addAll(components);
        for (Object target : targets) {
            notify(target, sdk);
        }
    }

    @Override
    public String toString() {
        return "ConfigurableComponentProvider{" + provider.getClass().getName() + '}';
    }

    // Each listener is called on its own, as autoconfigure's callAutoConfigureListeners does
    private static void notify(Object candidate, OpenTelemetrySdk sdk) {
        if (candidate instanceof AutoConfigureListener listener) {
            try {
                listener.afterAutoConfigure(sdk);
            } catch (Throwable t) {
                LOG.warnf(t, "Error invoking listener %s", listener.getClass().getName());
            }
        }
    }

    /**
     * Returns an adapter for each autoconfigure span, metric and log record exporter, sampler, propagator and
     * metric reader provider that {@code loader} loads. When two providers of one SPI have the same name, only the
     * one loaded last is adapted, as autoconfigure resolves names.
     *
     * @param loader the loader of the autoconfigure SPIs
     * @param config the flat {@code otel.*} properties passed to the providers
     * @return the adapters, in SPI load order
     */
    public static List<ComponentProvider> adaptAll(ComponentLoader loader, ConfigProperties config) {
        List<ComponentProvider> result = new ArrayList<>();
        adapt(result, loader, config, ConfigurableSpanExporterProvider.class, SpanExporter.class,
                ConfigurableSpanExporterProvider::getName, ConfigurableSpanExporterProvider::createExporter);
        adapt(result, loader, config, ConfigurableMetricExporterProvider.class, MetricExporter.class,
                ConfigurableMetricExporterProvider::getName, ConfigurableMetricExporterProvider::createExporter);
        adapt(result, loader, config, ConfigurableLogRecordExporterProvider.class, LogRecordExporter.class,
                ConfigurableLogRecordExporterProvider::getName, ConfigurableLogRecordExporterProvider::createExporter);
        adapt(result, loader, config, ConfigurableSamplerProvider.class, Sampler.class,
                ConfigurableSamplerProvider::getName, ConfigurableSamplerProvider::createSampler);
        adapt(result, loader, config, ConfigurablePropagatorProvider.class, TextMapPropagator.class,
                ConfigurablePropagatorProvider::getName, ConfigurablePropagatorProvider::getPropagator);
        adapt(result, loader, config, ConfigurableMetricReaderProvider.class, MetricReader.class,
                ConfigurableMetricReaderProvider::getName, ConfigurableMetricReaderProvider::createMetricReader);
        return result;
    }

    private static <S> void adapt(List<ComponentProvider> result, ComponentLoader loader, ConfigProperties config,
            Class<S> spiClass, Class<?> type, Function<S, String> getName, BiFunction<S, ConfigProperties, ?> factory) {
        // Autoconfigure's SpiHelper.loadConfigurable puts each provider into a map by name, so the last one wins
        Map<String, S> byName = new LinkedHashMap<>();
        for (S provider : loader.load(spiClass)) {
            byName.put(getName.apply(provider), provider);
        }
        byName.forEach((name, provider) -> result
                .add(new ConfigurableComponentProvider<>(type, name, provider, factory, config)));
    }
}
