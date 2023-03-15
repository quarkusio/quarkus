package io.quarkus.opentelemetry.runtime.exporter.otlp;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.DefaultBean;

@Deprecated
@Singleton
public class OtlpExporterProvider {
    @Produces
    @Singleton
    @DefaultBean
    public LateBoundBatchSpanProcessor batchSpanProcessorForOtlp() {
        return new LateBoundBatchSpanProcessor();
    }
}
