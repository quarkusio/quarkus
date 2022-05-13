package io.quarkus.opentelemetry.async.mutiny.runtime.tracing;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class SpanAttributeConfig {

    @ConfigItem(defaultValue = "true")
    public Boolean onCancellation;
}
