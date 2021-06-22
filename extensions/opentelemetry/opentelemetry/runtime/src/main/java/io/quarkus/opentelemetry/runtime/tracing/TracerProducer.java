package io.quarkus.opentelemetry.runtime.tracing;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.DefaultBean;

@Singleton
public class TracerProducer {
    @Produces
    @Singleton
    public DelayedAttributes getDelayedAttributes() {
        return new DelayedAttributes();
    }

    @Produces
    @Singleton
    public LateBoundSampler getLateBoundSampler() {
        return new LateBoundSampler();
    }

    @Produces
    @Singleton
    @DefaultBean
    public Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer("io.quarkus.opentelemetry");
    }
}
