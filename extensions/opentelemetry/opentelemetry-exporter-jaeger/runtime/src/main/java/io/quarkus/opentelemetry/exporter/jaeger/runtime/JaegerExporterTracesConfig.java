package io.quarkus.opentelemetry.exporter.jaeger.runtime;

import static io.quarkus.opentelemetry.exporter.jaeger.runtime.JaegerExporterRuntimeConfig.Constants.DEFAULT_JAEGER_ENDPOINT;

import java.time.Duration;
import java.util.Optional;

import io.quarkus.opentelemetry.runtime.tracing.config.TracesExporterConfig;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface JaegerExporterTracesConfig extends TracesExporterConfig {

    /**
     * Path part
     */
    Optional<String> endpoint();

    /**
     * Base endpoint + path.
     * Legacy opentelemetry.tracer.exporter.jaeger.endpoint:
     * {@link JaegerExporterRuntimeConfig.Constants#DEFAULT_JAEGER_ENDPOINT}
     */
    @Deprecated
    //    @WithConverter(TrimmedStringConverter.class)
    @WithDefault("${quarkus.opentelemetry.tracer.exporter.jaeger.endpoint:" + DEFAULT_JAEGER_ENDPOINT + "}")
    @WithName("legacy-endpoint")
    Optional<String> legacyEndpoint();

    /**
     * Sets the maximum time to wait for the collector to process an exported batch of spans. If
     * unset, defaults 10s.
     */
    @Override
    @WithDefault("${opentelemetry.tracer.exporter.jaeger.export-timeout:" + Constants.DEFAULT_TIMEOUT_SECS + "}")
    Duration timeout();

    @Override
    @WithDefault(Protocol.HTTP_THRIFT_BINARY)
    Optional<String> protocol();

    class Protocol {
        public static final String HTTP_THRIFT_BINARY = "http/thrift.binary";
        public static final String GRPC = "grpc";
        public static final String UDP_THRIFT_COMPACT = "udp/thrift.compact";
        public static final String UDP_THRIFT_BINARY = "udp/thrift.binary";
    }
}
