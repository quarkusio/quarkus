package io.quarkus.opentelemetry.runtime.config;

import java.util.Optional;

import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

public interface SpanConfig {
    /**
     * The maximum length of span attribute values. Takes precedence over otel.attribute.value.length.limit.
     * <p>
     * By default there is no limit.
     */
    @WithName("attribute.value.length.limit")
    Optional<Integer> attributeValueLengthLimit();

    /**
     * The maximum number of attributes per span. Takes precedence over otel.attribute.count.limit.
     * <p>
     * Default is 128.
     */
    @WithName("attribute.count.limit")
    @WithDefault("128")
    Integer attributeCountLimit();

    /**
     * The maximum number of events per span.
     * <p>
     * Default is 128.
     */
    @WithName("event.count.limit")
    @WithDefault("128")
    Integer eventCountLimit();

    /**
     * The maximum number of links per span.
     * <p>
     * Default is 128.
     */
    @WithName("link.count.limit")
    @WithDefault("128")
    Integer linkCountLimit();
}
