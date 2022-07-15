package io.quarkus.opentelemetry.runtime.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.opentelemetry.runtime.tracing.config.TracesRuntimeConfig;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "otel")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OtelRuntimeConfig {

    /**
     * If false, disable the OpenTelemetry SDK. Runtime configuration.
     * <p>
     * Defaults to true.
     */
    @WithDefault("true")
    @WithName("experimental.sdk.enabled")
    Boolean experimentalSdkEnabled();

    //    Boolean Enabled(); // quarkus only. Deploy config

    /**
     * Traces runtime config
     */
    TracesRuntimeConfig traces();

    //    ExporterType exporter(); // deploy config
    //    List<String> propagators(); // deploy config

    /**
     * environment variables for the types of attributes, for which that SDK implements truncation mechanism.
     */
    AttributeConfig attribute();

    /**
     * Span limit definitions
     */
    SpanConfig span();

    /**
     * Batch Span Processor configurations
     */
    //    @WithName("bsp")
    BatchSpanProcessorConfig bsp();

    /**
     * Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3
     */
    @WithName("resource.attributes")
    Map<String, String> resourceAttributes();

    /**
     * Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes
     * and from quarkus.application.name.
     * <p>
     * Defaults to quarkus.application.name
     */
    @WithDefault("${quarkus.application.name:unset}")
    @WithName("service.name")
    Optional<String> serviceName();

    /**
     * Specify resource attribute keys that are filtered.
     */
    @WithName("experimental.resource.disabled-keys")
    Optional<List<String>> experimentalResourceDisabledKeys();

}
