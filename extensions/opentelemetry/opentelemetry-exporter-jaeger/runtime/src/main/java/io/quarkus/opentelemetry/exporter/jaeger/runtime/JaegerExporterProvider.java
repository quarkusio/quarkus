package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;

@Singleton
public class JaegerExporterProvider {
    @Produces
    @Singleton
    @DefaultBean
    public LateBoundBatchSpanProcessor batchSpanProcessorForJaeger() {
        return new LateBoundBatchSpanProcessor();
    }
}
