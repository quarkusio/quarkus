package io.quarkus.opentelemetry.exporter.otlp.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

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
