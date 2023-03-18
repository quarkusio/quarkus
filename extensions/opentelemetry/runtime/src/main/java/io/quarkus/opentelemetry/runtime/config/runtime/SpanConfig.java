package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SpanConfig {

    /**
     * The maximum length of span attribute values. Takes precedence over otel.attribute.value.length.limit.
     * <p>
     * By default there is no limit.
     */
    @ConfigItem(name = "attribute.value.length.limit")
    Optional<Integer> attributeValueLengthLimit;

    /**
     * The maximum number of attributes per span. Takes precedence over otel.attribute.count.limit.
     * <p>
     * Default is 128.
     */
    @ConfigItem(name = "attribute.count.limit", defaultValue = "128")
    Integer attributeCountLimit;

    /**
     * The maximum number of events per span.
     * <p>
     * Default is 128.
     */
    @ConfigItem(name = "event.count.limit", defaultValue = "128")
    Integer eventCountLimit;

    /**
     * The maximum number of links per span.
     * <p>
     * Default is 128.
     */
    @ConfigItem(name = "link.count.limit", defaultValue = "128")
    Integer linkCountLimit;
}
