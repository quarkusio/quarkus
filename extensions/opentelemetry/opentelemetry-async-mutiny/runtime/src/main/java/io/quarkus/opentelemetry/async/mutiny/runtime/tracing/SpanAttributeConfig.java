package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SpanAttributeConfig {

    /**
     * Add a span attribute in case a mutiny stream is cancelled. It is a boolean attribute with key:
     * <code>mutiny.canceled</code>
     */
    @ConfigItem(defaultValue = "true")
    public Boolean onCancellation;
}
