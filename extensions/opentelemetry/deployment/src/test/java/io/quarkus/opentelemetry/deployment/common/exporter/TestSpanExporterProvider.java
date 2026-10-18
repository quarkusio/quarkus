package io.quarkus.opentelemetry.deployment.common.exporter;

import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.traces.ConfigurableSpanExporterProvider;
import io.opentelemetry.sdk.trace.export.SpanExporter;

public class TestSpanExporterProvider implements ConfigurableSpanExporterProvider {
    @Override
    public SpanExporter createExporter(final ConfigProperties config) {
        return CDI.current().select(TestSpanExporter.class).get();
    }

    @Override
    public String getName() {
        return "test-span-exporter";
    }
}
