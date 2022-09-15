package io.quarkus.it.opentelemetry;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;

@ApplicationScoped
public class SpanExporterProvider {

    @Produces
    @Singleton
    public InMemorySpanExporter createInMemoryExporter() {
        return InMemorySpanExporter.create();
    }
}
