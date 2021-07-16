package io.quarkus.jaeger.runtime;

import io.jaegertracing.spi.Reporter;

public interface ReporterFactory {

    Reporter createReporter(String endpoint);

}
