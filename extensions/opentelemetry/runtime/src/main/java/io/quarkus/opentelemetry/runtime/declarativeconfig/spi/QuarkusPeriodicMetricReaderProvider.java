package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.UnaryOperator;

import org.jboss.threads.ScheduledViewExecutor;
import org.jboss.threads.ViewExecutor;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.common.ComponentLoader;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReaderBuilder;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link MetricReader} component named {@link QuarkusOTelModelBuilder#QUARKUS_PERIODIC}: a
 * {@link PeriodicMetricReader} that exports through the metric exporter its {@code exporter} node names, every
 * {@code interval} milliseconds. When an executor is given, the reader runs on its own {@link ScheduledViewExecutor}
 * view of it instead of the unmanaged single-thread executor it creates itself.
 * <p>
 * The exporter customizer applies only when no executor is given. On the autoconfigure path, Quarkus replaced the
 * periodic reader with one on the managed executor that exported through the exporter as it was before the
 * customizers ran.
 * </p>
 * <p>
 * When no {@link MetricExporter} component has the exporter's name, the {@link MetricReader} component of that name
 * is returned instead, as autoconfigure resolved a name no metric exporter claims to a
 * {@code ConfigurableMetricReaderProvider}, such as Prometheus' pull reader. That reader keeps its own collection
 * schedule, so {@code interval} and the executor do not apply to it. The names of the Quarkus readers themselves
 * are not looked up this way.
 * </p>
 * <p>
 * Instances are not thread-safe; the SDK is built on one thread.
 * </p>
 */
public final class QuarkusPeriodicMetricReaderProvider implements ComponentProvider {

    private static final Set<String> QUARKUS_READERS = Set.of(QuarkusOTelModelBuilder.QUARKUS_PERIODIC,
            QuarkusOTelModelBuilder.QUARKUS_DEVUI);

    private final ScheduledExecutorService executor;
    private final UnaryOperator<MetricExporter> exporterCustomizer;

    /**
     * Creates the provider.
     *
     * @param executor the container-managed executor the readers get a view of, or {@code null} to let each reader
     *        create its own
     * @param exporterCustomizer applied to each resolved exporter before the reader wraps it when {@code executor} is
     *        {@code null}, not {@code null}
     */
    public QuarkusPeriodicMetricReaderProvider(ScheduledExecutorService executor,
            UnaryOperator<MetricExporter> exporterCustomizer) {
        this.executor = executor;
        this.exporterCustomizer = exporterCustomizer;
    }

    @Override
    public Class<?> getType() {
        return MetricReader.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_PERIODIC;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        DeclarativeConfigProperties exporterNode = config.getStructured("exporter");
        String name = Components.name(exporterNode, QuarkusOTelModelBuilder.QUARKUS_PERIODIC + " metric reader's exporter");
        DeclarativeConfigProperties exporterConfig = Components.config(config, exporterNode, name);
        ComponentLoader componentLoader = config.getComponentLoader();

        MetricExporter exporter = (MetricExporter) Components.create(componentLoader, MetricExporter.class, name,
                exporterConfig);
        if (exporter == null) {
            Object reader = QUARKUS_READERS.contains(name) ? null
                    : Components.create(componentLoader, MetricReader.class, name, exporterConfig);
            if (reader == null) {
                throw Components.noSuchComponent(MetricExporter.class, name);
            }
            return reader;
        }

        MetricExporter used = exporter;
        try {
            if (executor == null) {
                used = exporterCustomizer.apply(exporter);
            }
            PeriodicMetricReaderBuilder builder = PeriodicMetricReader.builder(used);
            Long interval = config.getLong("interval");
            if (interval != null) {
                builder.setInterval(Duration.ofMillis(interval));
            }
            if (executor != null) {
                // The reader assumes it owns its executor and shuts it down on close; the view drains this reader's
                // tasks without touching the shared pool.
                builder.setExecutor(new ScheduledViewExecutor(
                        ViewExecutor.builder(executor)
                                .unlimitedParallelism()
                                .unlimitedQueueLimit()
                                .supportInterruptOnShutdownNow(true)
                                .build(),
                        executor));
            }
            return builder.build();
        } catch (RuntimeException e) {
            // As autoconfigure and upstream: the exporters already created are closed when configuration fails
            close(used, e);
            if (used != exporter) {
                close(exporter, e);
            }
            throw e;
        }
    }

    private static void close(MetricExporter exporter, RuntimeException failure) {
        try {
            exporter.close();
        } catch (RuntimeException closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }
}
