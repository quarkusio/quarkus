package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import java.util.Optional;

import io.quarkus.opentelemetry.runtime.config.OtelConnectionConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "otel.exporter.jaeger")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface JaegerExporterRuntimeConfig extends OtelConnectionConfig {

    /**
     * Sets the Jaeger base endpoint to connect to. If unset, defaults to {@value Constants#DEFAULT_JAEGER_ENDPOINT}.
     */
    @Override
    //    @WithConverter(TrimmedStringConverter.class)
    Optional<String> endpoint();

    /**
     * Jaeger traces exporter configuration
     */
    JaegerExporterTracesConfig traces();
    // TODO metrics();
    // TODO logs();

    class Constants {
        public static final String DEFAULT_JAEGER_ENDPOINT = "http://localhost:14250";
    }
}
