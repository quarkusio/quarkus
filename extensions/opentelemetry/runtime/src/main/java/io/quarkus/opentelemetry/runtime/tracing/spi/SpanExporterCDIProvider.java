package io.quarkus.opentelemetry.runtime.tracing.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.api.incubator.config.DeclarativeConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.ComponentProvider;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Generic Span Export provider for CDI beans.
 * <p>
 * Implements both {@link ConfigurableSpanExporterProvider} (the legacy, property-based autoconfigure SPI, resolved
 * by name "cdi") and {@link ComponentProvider} (the declarative-config SPI, resolved by the same name) so the same
 * CDI lookup backs both SDK initialization paths.
 * </p>
 */
public class SpanExporterCDIProvider implements ConfigurableSpanExporterProvider, ComponentProvider {

    @Override
    public SpanExporter createExporter(ConfigProperties configProperties) {
        return createExporter();
    }

    @Override
    public Class<?> getType() {
        return SpanExporter.class;
    }

    @Override
    public Object create(DeclarativeConfigProperties config) {
        return createExporter();
    }

    @Override
    public String getName() {
        return CDI_VALUE;
    }

    private SpanExporter createExporter() {
        return SpanExporter.composite(CDI.current().select(SpanExporter.class, Any.Literal.INSTANCE));
    }
}
