package io.quarkus.opentelemetry.runtime.config.runtime.exporter;

import static io.quarkus.opentelemetry.runtime.config.runtime.exporter.OtlpExporterRuntimeConfig.DEFAULT_GRPC_BASE_URI;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface OtlpExporterTracesConfig extends OtlpExporterConfig {

    /**
     * See {@link OtlpExporterTracesConfig#endpoint}
     */
    //    @WithConverter(TrimmedStringConverter.class)
    @Deprecated
    @WithName("legacy-endpoint")
    @WithDefault(DEFAULT_GRPC_BASE_URI)
    Optional<String> legacyEndpoint();
}
