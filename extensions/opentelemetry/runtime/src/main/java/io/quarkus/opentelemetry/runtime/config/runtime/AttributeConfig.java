package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AttributeConfig {

    /**
     * The maximum length of attribute values. Applies to spans and logs.
     * <p>
     * By default there is no limit.
     */
    @ConfigItem(name = "value.length.limit")
    Optional<String> valueLengthLimit;

    /**
     * The maximum number of attributes. Applies to spans, span events, span links, and logs.
     * <p>
     * Default is 128.
     */
    @ConfigItem(name = "count.limit", defaultValue = "128")
    Integer countLimit;
}
