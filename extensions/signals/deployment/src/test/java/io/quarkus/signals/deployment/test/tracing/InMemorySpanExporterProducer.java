package io.quarkus.signals.deployment.test.tracing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;

@ApplicationScoped
public class InMemorySpanExporterProducer {

    @Produces
    @Singleton
    InMemorySpanExporter inMemorySpanExporter() {
        return InMemorySpanExporter.create();
    }

}
