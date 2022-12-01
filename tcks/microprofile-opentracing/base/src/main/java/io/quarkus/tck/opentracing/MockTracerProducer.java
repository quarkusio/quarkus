package io.quarkus.tck.opentracing;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;
import javax.interceptor.Interceptor;

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
