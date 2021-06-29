package io.quarkus.opentelemetry.runtime.tracing;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "opentelemetry.tracer", phase = ConfigPhase.RUN_TIME)
public class TracerRuntimeConfig {

    /**
     * A comma separated list of name=value resource attributes that
     * represents the entity producing telemetry
     * (eg. {@code service.name=authservice}).
     */
    @ConfigItem
    Optional<List<String>> resourceAttributes;

    /** Config for sampler */
    public SamplerConfig sampler;

    /**
     * Suppress non-application uris from trace collection.
     * This will suppress tracing of `/q` endpoints.
     * <p>
     * Providing a custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean
     * will ignore this setting.
     * <p>
     * Suppressing non-application uris is enabled by default.
     */
    @ConfigItem(defaultValue = "true")
    public boolean suppressNonApplicationUris;

    @ConfigGroup
    public static class SamplerConfig {
        /**
         * The sampler to use for tracing
         * <p>
         * Valid values are {@code off, on, ratio}.
         * <p>
         * Defaults to {@code on}.
         */
        @ConfigItem(name = ConfigItem.PARENT, defaultValue = "on")
        public String samplerName;

        /**
         * The sampler ratio to use for tracing
         * <p>
         * Only supported by the {@code ratio} sampler.
         */
        public Optional<Double> ratio;

        /**
         * If the sampler to use for tracing is parent based
         * <p>
         * Valid values are {@code true, false}.
         * <p>
         * Defaults to {@code true}.
         */
        @ConfigItem(defaultValue = "true")
        public Boolean parentBased;
    }
}
