package io.quarkus.it.opentracing;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.opentracing.mock.MockTracer;
import io.opentracing.util.GlobalTracer;

@ApplicationScoped
public class MockTracerProvider {

    @Produces
    @Singleton
    @Alternative
    @Priority(1)
    public MockTracer createInMemoryExporter() {
        MockTracer tracer = new MockTracer();
        GlobalTracer.registerIfAbsent(tracer);
        return tracer;
    }
}
