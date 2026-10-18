package io.quarkus.opentelemetry.deployment.common.exporter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Any;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import io.quarkus.arc.Unremovable;

@Unremovable
@ApplicationScoped
public class InMemoryExporter {

    @Inject
    @Any
    Instance<TestSpanExporter> testSpanExporter;

    @Inject
    @Any
    Instance<InMemoryMetricExporter> inMemoryMetricExporter;

    @Inject
    @Any
    Instance<InMemoryLogRecordExporter> inMemoryLogRecordExporter;

    public void reset() {
        if (testSpanExporter.isResolvable()) {
            testSpanExporter.get().reset();
        }
        if (inMemoryMetricExporter.isResolvable()) {
            inMemoryMetricExporter.get().reset();
        }
        if (inMemoryLogRecordExporter.isResolvable()) {
            inMemoryLogRecordExporter.get().reset();
        }
    }

    public TestSpanExporter getSpanExporter() {
        return testSpanExporter.get();
    }

    public InMemoryMetricExporter getMetricExporter() {
        return inMemoryMetricExporter.get();
    }

    public InMemoryLogRecordExporter getLogRecordExporter() {
        return inMemoryLogRecordExporter.get();
    }
}
