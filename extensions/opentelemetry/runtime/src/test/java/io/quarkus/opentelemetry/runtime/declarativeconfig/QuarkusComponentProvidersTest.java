package io.quarkus.opentelemetry.runtime.declarativeconfig;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;
import static io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder.QUARKUS_DROP_TARGETS;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.junit.jupiter.api.Test;

import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.quarkus.opentelemetry.runtime.declarativeconfig.spi.QuarkusDropTargetsSamplerProvider;
import io.quarkus.opentelemetry.runtime.logs.spi.LogsExporterCDIProvider;
import io.quarkus.opentelemetry.runtime.metrics.spi.MetricsExporterCDIProvider;
import io.quarkus.opentelemetry.runtime.tracing.spi.SpanExporterCDIProvider;

/**
 * Verifies the {@code ComponentProvider} registrations themselves, without exercising CDI or building a sampler:
 * that {@link SpanExporterCDIProvider}/{@link MetricsExporterCDIProvider}/{@link LogsExporterCDIProvider} - which
 * also implement the legacy, property-based {@code ConfigurableXxxExporterProvider} SPIs - are discoverable under
 * {@link SpanExporter}/{@link MetricExporter}/{@link LogRecordExporter} with the name {@code cdi}; that
 * {@link QuarkusDropTargetsSamplerProvider} is discoverable under {@link Sampler} with the name
 * {@code quarkus_drop_targets}; and that no two providers on the classpath claim the same (type, name) pair - the
 * exact ambiguity that {@code DeclarativeConfigContext.loadComponent()} rejects with "Component provider conflict"
 * at SDK build time.
 */
class QuarkusComponentProvidersTest {

    @Test
    void cdiBridgeProvidersAreRegisteredForEachSignal() {
        List<ComponentProvider> providers = loadProviders();

        assertThat(providers)
                .filteredOn(provider -> provider.getType() == SpanExporter.class && CDI_VALUE.equals(provider.getName()))
                .hasSize(1)
                .first().isInstanceOf(SpanExporterCDIProvider.class);
        assertThat(providers)
                .filteredOn(provider -> provider.getType() == MetricExporter.class && CDI_VALUE.equals(provider.getName()))
                .hasSize(1)
                .first().isInstanceOf(MetricsExporterCDIProvider.class);
        assertThat(providers)
                .filteredOn(provider -> provider.getType() == LogRecordExporter.class && CDI_VALUE.equals(provider.getName()))
                .hasSize(1)
                .first().isInstanceOf(LogsExporterCDIProvider.class);
    }

    @Test
    void dropTargetsSamplerProviderIsRegistered() {
        List<ComponentProvider> providers = loadProviders();

        assertThat(providers)
                .filteredOn(provider -> provider.getType() == Sampler.class && QUARKUS_DROP_TARGETS.equals(provider.getName()))
                .hasSize(1)
                .first().isInstanceOf(QuarkusDropTargetsSamplerProvider.class);
    }

    @Test
    void noComponentProviderNameCollisionsOnTheClasspath() {
        Map<String, List<Class<?>>> providersByKey = new HashMap<>();
        for (ComponentProvider provider : loadProviders()) {
            String key = provider.getType().getName() + "/" + provider.getName();
            providersByKey.computeIfAbsent(key, ignored -> new ArrayList<>()).add(provider.getClass());
        }

        Map<String, List<Class<?>>> collisions = new HashMap<>();
        providersByKey.forEach((key, implementations) -> {
            if (implementations.size() > 1) {
                collisions.put(key, implementations);
            }
        });

        assertThat(collisions).as("Duplicate (type, name) ComponentProvider registrations").isEmpty();
    }

    private static List<ComponentProvider> loadProviders() {
        List<ComponentProvider> providers = new ArrayList<>();
        ServiceLoader.load(ComponentProvider.class, QuarkusComponentProvidersTest.class.getClassLoader())
                .forEach(providers::add);
        return providers;
    }
}
