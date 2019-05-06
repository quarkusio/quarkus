package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

/**
 * JSON-specific logging configuration.
 */
@ConfigGroup
public class JsonConfig {
    /**
     * Enable logging of call details (method name, class name, etc.) into the JSON output.
     */
    @ConfigItem
    public boolean callDetails;
}
