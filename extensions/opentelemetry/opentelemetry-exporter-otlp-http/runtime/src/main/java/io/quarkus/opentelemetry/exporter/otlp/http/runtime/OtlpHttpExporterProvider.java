package io.quarkus.opentelemetry.exporter.otlp.http.runtime;

import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.DefaultBean;

@Singleton
public class OtlpHttpExporterProvider {
    @Produces
    @Singleton
    @DefaultBean
    public LateBoundBatchSpanProcessor batchSpanProcessorForOtlpHttp() {
        return new LateBoundBatchSpanProcessor();
    }
}
