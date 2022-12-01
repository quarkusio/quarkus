package io.quarkus.opentelemetry.runtime.exporter.otlp;

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
