package io.quarkus.opentelemetry.runtime.config.build;

import java.util.Optional;

import io.quarkus.opentelemetry.runtime.exporter.otlp.tracing.EndUserSpanProcessor;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

/**
 * Tracing build time configuration
 */
@ConfigGroup
public interface EndUserSpanProcessorConfig {

    /**
     * Enable the {@link EndUserSpanProcessor}.
     * <p>
     * The {@link EndUserSpanProcessor} adds
     * the {@link io.opentelemetry.semconv.SemanticAttributes.ENDUSER_ID}
     * and {@link io.opentelemetry.semconv.SemanticAttributes.ENDUSER_ROLE} to the Span.
     */
    @WithDefault("false")
    Optional<Boolean> enabled();

}
