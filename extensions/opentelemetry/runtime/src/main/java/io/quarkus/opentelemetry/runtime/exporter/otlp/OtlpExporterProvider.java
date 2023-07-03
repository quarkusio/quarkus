package io.quarkus.opentelemetry.runtime.exporter.otlp;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

@Deprecated
@Singleton
public class OtlpExporterProvider {
    @Produces
    @Singleton
    public LateBoundBatchSpanProcessor batchSpanProcessorForOtlp() {
        return new LateBoundBatchSpanProcessor();
    }
}
