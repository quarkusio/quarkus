package io.quarkus.opentelemetry.tracing;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import io.quarkus.arc.DefaultBean;

@Singleton
public class TracerProducer {
    @Produces
    @Singleton
    @DefaultBean
    public Tracer getTracer() {
        return GlobalOpenTelemetry.getTracer("io.quarkus.opentelemetry");
    }
}
