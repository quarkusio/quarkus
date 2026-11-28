package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.logs.LogRecordProcessor;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.logs.export.SimpleLogRecordProcessor;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link LogRecordProcessor} component named {@link QuarkusOTelModelBuilder#QUARKUS_SIMPLE} to a
 * {@link SimpleLogRecordProcessor} around the application's default {@code LogRecordExporter} bean, used as-is. The
 * processor is created without the SDK meter provider, so it records no processor self-metrics, as on the
 * autoconfigure path. The node's configuration is not used.
 */
public final class QuarkusSimpleLogRecordProcessorProvider implements ComponentProvider {

    private final LogRecordExporter exporter;

    /**
     * Creates the provider.
     *
     * @param exporter the exporter of every processor {@link #create(DeclarativeConfigProperties)} returns, not
     *        {@code null}
     */
    public QuarkusSimpleLogRecordProcessorProvider(LogRecordExporter exporter) {
        this.exporter = exporter;
    }

    @Override
    public Class<?> getType() {
        return LogRecordProcessor.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_SIMPLE;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return SimpleLogRecordProcessor.create(exporter);
    }
}
