package io.quarkus.it.opentracing;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;
import io.quarkus.arc.AlternativePriority;

@ApplicationScoped
public class MockTracerProvider {

    @Produces
    @Singleton
    @AlternativePriority(1)
    public MockTracer createInMemoryExporter() {
        MockTracer tracer = new MockTracer();
        GlobalTracer.registerIfAbsent(tracer);
        return tracer;
    }
}
