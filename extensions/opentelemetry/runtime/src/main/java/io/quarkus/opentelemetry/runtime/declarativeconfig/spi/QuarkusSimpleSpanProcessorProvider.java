package io.quarkus.opentelemetry.runtime.declarativeconfig.spi;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import io.quarkus.opentelemetry.runtime.declarativeconfig.QuarkusOTelModelBuilder;

/**
 * Resolves the {@link SpanProcessor} component named {@link QuarkusOTelModelBuilder#QUARKUS_SIMPLE}: a
 * {@link SimpleSpanProcessor} around the exporter the node's {@code exporter} entry names. Unlike the {@code simple}
 * processor of the model, it is created without the SDK meter provider, so it records no processor self-metrics, as
 * on the autoconfigure path.
 */
public final class QuarkusSimpleSpanProcessorProvider implements ComponentProvider {

    @Override
    public Class<?> getType() {
        return SpanProcessor.class;
    }

    @Override
    public String getName() {
        return QuarkusOTelModelBuilder.QUARKUS_SIMPLE;
    }

    /**
     * {@inheritDoc}
     *
     * @throws io.opentelemetry.api.incubator.config.DeclarativeConfigException if the {@code exporter} entry does
     *         not name exactly one exporter, or no exporter has that name
     */
    @Override
    public Object create(DeclarativeConfigProperties config) {
        DeclarativeConfigProperties exporterNode = config.getStructured("exporter");
        String name = Components.name(exporterNode, QuarkusOTelModelBuilder.QUARKUS_SIMPLE + " span processor's exporter");
        SpanExporter exporter = (SpanExporter) Components.create(config.getComponentLoader(), SpanExporter.class, name,
                Components.config(config, exporterNode, name));
        if (exporter == null) {
            throw Components.noSuchComponent(SpanExporter.class, name);
        }
        return SimpleSpanProcessor.create(exporter);
    }
}
