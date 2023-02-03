package io.quarkus.tck.opentracing;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Default;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import jakarta.interceptor.Interceptor;

import io.opentracing.Tracer;
import io.opentracing.mock.MockTracer;

/**
 * @author Pavol Loffay
 * @author Jan Martiska
 */
@ApplicationScoped
@Alternative // this needs to override io.quarkus.smallrye.opentracing.runtime.TracerProducer
@Priority(Interceptor.Priority.APPLICATION + 10)
public class MockTracerProducer {

    @Default
    @Produces
    @Singleton
    public Tracer tracer() {
        return new MockTracer();
    }
}
