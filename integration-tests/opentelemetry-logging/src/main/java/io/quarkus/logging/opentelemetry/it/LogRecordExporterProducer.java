package io.quarkus.logging.opentelemetry.it;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.quarkus.arc.Unremovable;

@ApplicationScoped
public class LogRecordExporterProducer {
    @Produces
    @Singleton
    @Unremovable
    public InMemoryLogRecordExporter createInMemoryExporter() {
        return InMemoryLogRecordExporter.create();
    }
}
