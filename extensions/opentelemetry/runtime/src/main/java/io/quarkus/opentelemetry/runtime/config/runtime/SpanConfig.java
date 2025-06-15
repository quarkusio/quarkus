package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface SpanConfig {

    /**
     * The maximum length of span attribute values. Takes precedence over
     * <code>otel.attribute.value.length.limit</code>.
     * <p>
     * By default, there is no limit.
     */
    @WithName("attribute.value.length.limit")
    Optional<Integer> attributeValueLengthLimit();

    /**
     * The maximum number of attributes per span. Takes precedence over <code>otel.attribute.count.limit</code>.
     * <p>
     * Default is `128`.
     */
    @WithName("attribute.count.limit")
    @WithDefault("128")
    Integer attributeCountLimit();

    /**
     * The maximum number of events per span.
     * <p>
     * Default is `128`.
     */
    @WithName("event.count.limit")
    @WithDefault("128")
    Integer eventCountLimit();

    /**
     * The maximum number of links per span.
     * <p>
     * Default is `128`.
     */
    @WithName("link.count.limit")
    @WithDefault("128")
    Integer linkCountLimit();
}
