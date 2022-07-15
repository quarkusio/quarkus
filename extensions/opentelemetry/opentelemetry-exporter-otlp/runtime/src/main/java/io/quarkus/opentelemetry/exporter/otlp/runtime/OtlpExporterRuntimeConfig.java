package io.quarkus.opentelemetry.exporter.otlp.runtime;

import static io.quarkus.opentelemetry.exporter.otlp.runtime.OtlpExporterRuntimeConfig.Constants.*;

import java.util.Optional;

import io.quarkus.opentelemetry.runtime.config.OtelConnectionConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "otel.exporter.otlp")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OtlpExporterRuntimeConfig extends OtelConnectionConfig {

    /**
     * Sets the OTLP endpoint to connect to. If unset, defaults to {@value Constants#DEFAULT_GRPC_BASE_URL}.
     */
    @Override
    //    @WithConverter(TrimmedStringConverter.class)
    @WithDefault(DEFAULT_GRPC_BASE_URL)
    Optional<String> endpoint();

    /**
     * OTLP traces exporter configuration
     */
    OtlpExporterTracesConfig traces();
    // TODO metrics();
    // TODO logs();

    /**
     * From <a href=
     * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#configuration-options">
     * the OpenTelemetry Protocol Exporter configuration options</a>
     */
    class Constants {
        public static final String DEFAULT_GRPC_BASE_URL = "http://localhost:4317/";
        public static final String DEFAULT_HTTP_BASE_URL = "http://localhost:4318/";
    }
}
