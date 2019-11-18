package io.quarkus.smallrye.opentracing.deployment;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;

import io.opentracing.util.GlobalTracer;
import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class TracerRegistrar {

    public void start(@Observes StartupEvent start) {
        GlobalTracer.register(TracingTest.mockTracer);
    }
}
