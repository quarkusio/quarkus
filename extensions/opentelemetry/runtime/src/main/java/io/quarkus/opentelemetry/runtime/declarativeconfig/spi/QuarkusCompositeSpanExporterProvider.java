package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.List;
import java.util.function.UnaryOperator;

import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link SpanExporter} component named {@link QuarkusOTelModelBuilder#QUARKUS_COMPOSITE}: one exporter
 * over the exporters the node's {@code exporters} list names, each resolved by name and passed through the customizer
 * before they are composed. A single exporter is not wrapped.
 * <p>
 * When resolving or customizing an exporter fails, the exporters already built are closed before the exception
 * propagates.
 * </p>
 */
public final class QuarkusCompositeSpanExporterProvider extends CompositeExporterProvider<SpanExporter> {

    /**
     * Creates the provider the {@code ComponentProvider} service file registers, which does not customize the
     * exporters.
     */
    public QuarkusCompositeSpanExporterProvider() {
        this(UnaryOperator.identity());
    }

    /**
     * Creates the provider.
     *
     * @param customizer applied to each exporter before they are composed, not {@code null}
     */
    public QuarkusCompositeSpanExporterProvider(UnaryOperator<SpanExporter> customizer) {
        super(SpanExporter.class, customizer);
    }

    @Override
    SpanExporter compose(List<SpanExporter> exporters) {
        return SpanExporter.composite(exporters);
    }
}
