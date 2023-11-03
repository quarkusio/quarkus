package io.quarkus.opentelemetry.runtime.tracing.spi;

import static io.quarkus.opentelemetry.runtime.config.build.ExporterType.Constants.CDI_VALUE;

import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * Generic Span Export provider for CDI beans
 */
public class SpanExporterCDIProvider implements ConfigurableSpanExporterProvider {

    @Override
    public SpanExporter createExporter(ConfigProperties configProperties) {
        return SpanExporter.composite(CDI.current().select(SpanExporter.class, Any.Literal.INSTANCE));
    }

    @Override
    public String getName() {
        return CDI_VALUE;
    }
}
