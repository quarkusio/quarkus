package io.quarkus.opentelemetry.runtime.tracing.config;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface TracesRuntimeConfig {

    /**
     * Quarkus specific properties
     */
    ExtraConfigs extra();

    interface ExtraConfigs {

        /**
         * Suppress non-application uris from trace collection.
         * This will suppress tracing of `/q` endpoints.
         * <p>
         * Providing a custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean
         * will ignore this setting.
         * <p>
         * This is a Quarkus specific property.
         * The legacy property was: quarkus.opentelemetry.tracer.suppress-non-application-uris
         * <p>
         * Suppressing non-application uris is enabled by default.
         */
        @WithDefault("${opentelemetry.tracer.suppress-non-application-uris:true}")
        @WithName("suppress-non-application-uris")
        Boolean suppressNonApplicationUris();

        /**
         * Include static resources from trace collection.
         * <p>
         * This is a Quarkus specific property.
         * Providing a custom {@code io.opentelemetry.sdk.trace.samplers.Sampler} CDI Bean
         * will ignore this setting.
         * <p>
         * Include static resources is disabled by default.
         */
        @WithDefault("${quarkus.opentelemetry.tracer.include-static-resources:false}")
        @WithName("include-static-resources")
        Boolean includeStaticResources();
    }
}
