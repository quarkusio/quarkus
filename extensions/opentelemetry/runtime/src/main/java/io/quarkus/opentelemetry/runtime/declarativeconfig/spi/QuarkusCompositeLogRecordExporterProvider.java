package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import java.util.List;
import java.util.function.UnaryOperator;

import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link LogRecordExporter} component named {@link QuarkusOTelModelBuilder#QUARKUS_COMPOSITE}: one
 * exporter over the exporters the node's {@code exporters} list names, each resolved by name and passed through the
 * customizer before they are composed. A single exporter is not wrapped.
 * <p>
 * When resolving or customizing an exporter fails, the exporters already built are closed before the exception
 * propagates.
 * </p>
 */
public final class QuarkusCompositeLogRecordExporterProvider extends CompositeExporterProvider<LogRecordExporter> {

    /**
     * Creates the provider the {@code ComponentProvider} service file registers, which does not customize the
     * exporters.
     */
    public QuarkusCompositeLogRecordExporterProvider() {
        this(UnaryOperator.identity());
    }

    /**
     * Creates the provider.
     *
     * @param customizer applied to each exporter before they are composed, not {@code null}
     */
    public QuarkusCompositeLogRecordExporterProvider(UnaryOperator<LogRecordExporter> customizer) {
        super(LogRecordExporter.class, customizer);
    }

    @Override
    LogRecordExporter compose(List<LogRecordExporter> exporters) {
        return LogRecordExporter.composite(exporters);
    }
}
