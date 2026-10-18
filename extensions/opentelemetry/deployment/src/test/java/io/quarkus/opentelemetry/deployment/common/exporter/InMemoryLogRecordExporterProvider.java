package io.quarkus.opentelemetry.deployment.common.exporter;

import jakarta.enterprise.inject.spi.CDI;

import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.logs.ConfigurableLogRecordExporterProvider;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;

public class InMemoryLogRecordExporterProvider implements ConfigurableLogRecordExporterProvider {
    @Override
    public LogRecordExporter createExporter(ConfigProperties configProperties) {
        return CDI.current().select(InMemoryLogRecordExporter.class).get();
    }

    @Override
    public String getName() {
        return "in-memory";
    }
}
