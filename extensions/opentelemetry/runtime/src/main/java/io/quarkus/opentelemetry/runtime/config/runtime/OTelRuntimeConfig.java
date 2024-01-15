package io.quarkus.opentelemetry.runtime.config.runtime;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.otel")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface OTelRuntimeConfig {

    /**
     * If true, disable the OpenTelemetry SDK. Runtime configuration.
     * <p>
     * Defaults to <code>false</code>.
     */
    @WithName("sdk.disabled")
    @WithDefault("false")
    boolean sdkDisabled();

    /**
     * Traces runtime config.
     */
    TracesRuntimeConfig traces();

    /**
     * environment variables for the types of attributes, for which that SDK implements truncation mechanism.
     */
    AttributeConfig attribute();

    /**
     * Span limit definitions.
     */
    SpanConfig span();

    /**
     * Batch Span Processor configurations.
     */
    BatchSpanProcessorConfig bsp();

    /**
     * Specify resource attributes in the following format: <code>key1=val1,key2=val2,key3=val3</code>.
     */
    @WithName("resource.attributes")
    Optional<List<String>> resourceAttributes();

    /**
     * Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes
     * and from quarkus.application.name.
     * <p>
     * Defaults to <code>quarkus.application.name</code>.
     */
    @WithName("service.name")
    @WithDefault("${quarkus.application.name:unset}")
    Optional<String> serviceName();

    /**
     * Specify resource attribute keys that are filtered.
     */
    @WithName("experimental.resource.disabled-keys")
    Optional<List<String>> experimentalResourceDisabledKeys();

    /**
     * The maximum amount of time Quarkus will wait for the OpenTelemetry SDK to flush unsent spans and shutdown.
     */
    @WithName("experimental.shutdown-wait-time")
    @WithDefault("1s")
    Duration experimentalShutdownWaitTime();

    /**
     * Enable/disable instrumentation for specific technologies.
     */
    InstrumentRuntimeConfig instrument();
}
