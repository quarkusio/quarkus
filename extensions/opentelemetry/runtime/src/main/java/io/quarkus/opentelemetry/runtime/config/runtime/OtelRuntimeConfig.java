package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "otel", phase = ConfigPhase.RUN_TIME)
public class OtelRuntimeConfig {

    /**
     * If true, disable the OpenTelemetry SDK. Runtime configuration.
     * <p>
     * Defaults to false.
     */
    @ConfigItem(name = "sdk.disabled", defaultValue = "false")
    public boolean sdkDisabled;

    /**
     * Traces runtime config
     */
    public TracesRuntimeConfig traces;

    /**
     * environment variables for the types of attributes, for which that SDK implements truncation mechanism.
     */
    public AttributeConfig attribute;

    /**
     * Span limit definitions
     */
    public SpanConfig span;

    /**
     * Batch Span Processor configurations
     */
    public BatchSpanProcessorConfig bsp;

    /**
     * Specify resource attributes in the following format: key1=val1,key2=val2,key3=val3
     */
    @ConfigItem(name = "resource.attributes")
    public Optional<List<String>> resourceAttributes;

    /**
     * Specify logical service name. Takes precedence over service.name defined with otel.resource.attributes
     * and from quarkus.application.name.
     * <p>
     * Defaults to quarkus.application.name
     */
    @ConfigItem(name = "service.name", defaultValue = "${quarkus.application.name:unset}")
    public Optional<String> serviceName;

    /**
     * Specify resource attribute keys that are filtered.
     */
    @ConfigItem(name = "experimental.resource.disabled-keys")
    public Optional<List<String>> experimentalResourceDisabledKeys;
}
