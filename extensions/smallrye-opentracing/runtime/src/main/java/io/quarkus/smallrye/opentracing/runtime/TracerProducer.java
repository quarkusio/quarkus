package io.quarkus.smallrye.opentracing.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;

import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;

/**
 * Created by bob on 8/6/18.
 */
@Dependent
public class TracerProducer {
    @Produces
    @ApplicationScoped
    Tracer tracer() {
        return GlobalTracer.get();
    }
}
