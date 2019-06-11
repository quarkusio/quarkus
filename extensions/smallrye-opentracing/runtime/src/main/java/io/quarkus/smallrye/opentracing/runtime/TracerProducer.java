package io.quarkus.smallrye.opentracing.runtime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Produces;

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
