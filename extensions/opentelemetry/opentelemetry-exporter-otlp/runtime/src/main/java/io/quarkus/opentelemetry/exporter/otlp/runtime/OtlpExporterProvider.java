package io.quarkus.opentelemetry.exporter.otlp.runtime;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;

@Singleton
public class OtlpExporterProvider {
    @Produces
    @Singleton
    @DefaultBean
    public LateBoundBatchSpanProcessor batchSpanProcessorForJaeger() {
        return new LateBoundBatchSpanProcessor();
    }
}
