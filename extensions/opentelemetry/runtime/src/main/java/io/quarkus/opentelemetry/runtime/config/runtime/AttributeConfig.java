package io.quarkus.opentelemetry.runtime.config.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigGroup
public interface AttributeConfig {

    /**
     * The maximum length of attribute values. Applies to spans and logs.
     * <p>
     * By default, there is no limit.
     */
    @WithName("value.length.limit")
    Optional<String> valueLengthLimit();

    /**
     * The maximum number of attributes. Applies to spans, span events, span links, and logs.
     * <p>
     * Default is `128`.
     */
    @WithName("count.limit")
    @WithDefault("128")
    Integer countLimit();
}
