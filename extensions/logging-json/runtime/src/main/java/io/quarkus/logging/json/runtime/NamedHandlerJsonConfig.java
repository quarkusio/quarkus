package io.quarkus.logging.json.runtime;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * Configuration for JSON log formatting.
 */
@ConfigGroup
public class NamedHandlerJsonConfig {
    /**
     * Json formatter configuration for named handler
     */
    @ConfigItem(name = "json")
    JsonConfig jsonConfig;
}
