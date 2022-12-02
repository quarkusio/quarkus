package io.quarkus.opentelemetry.runtime.exporter.otlp;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.annotations.ConvertWith;
import io.quarkus.runtime.configuration.TrimmedStringConverter;

public class OtlpExporterConfig {
    @ConfigRoot(name = "opentelemetry.tracer.exporter.otlp", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
    public static class OtlpExporterBuildConfig {
        /**
         * OTLP SpanExporter support.
         * <p>
         * OTLP SpanExporter support is enabled by default.
         */
        @ConfigItem(defaultValue = "true")
        public Boolean enabled;
    }

    @ConfigRoot(name = "opentelemetry.tracer.exporter.otlp", phase = ConfigPhase.RUN_TIME)
    public static class OtlpExporterRuntimeConfig {
        /**
         * The OTLP endpoint to connect to. The endpoint must start with either http:// or https://.
         */
        @ConfigItem
        @ConvertWith(TrimmedStringConverter.class)
        public Optional<String> endpoint;

        /**
         * Key-value pairs to be used as headers associated with gRPC requests.
         * The format is similar to the {@code OTEL_EXPORTER_OTLP_HEADERS} environment variable,
         * a list of key-value pairs separated by the "=" character.
         * See <a href=
         * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#specifying-headers-via-environment-variables">
         * Specifying headers</a> for more details.
         */
        @ConfigItem
        Optional<List<String>> headers;

        /**
         * The maximum amount of time to wait for the collector to process exported spans before an exception is thrown.
         * A value of `0` will disable the timeout: the exporter will continue waiting until either exported spans are
         * processed,
         * or the connection fails, or is closed for some other reason.
         */
        @ConfigItem(defaultValue = "10S")
        public Duration exportTimeout;

        /**
         * Compression method to be used by exporter to compress the payload.
         * <p>
         * See <a href=
         * "https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/protocol/exporter.md#opentelemetry-protocol-exporter">
         * Configuration Options</a> for the supported compression methods.
         */
        @ConfigItem
        public Optional<String> compression;

    }
}
