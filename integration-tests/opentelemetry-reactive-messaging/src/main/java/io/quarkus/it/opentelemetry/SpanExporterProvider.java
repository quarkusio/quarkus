package io.quarkus.it.opentelemetry;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;

@ApplicationScoped
public class SpanExporterProvider {

    @Produces
    @Singleton
    public InMemorySpanExporter createInMemoryExporter() {
        return InMemorySpanExporter.create();
    }
}
