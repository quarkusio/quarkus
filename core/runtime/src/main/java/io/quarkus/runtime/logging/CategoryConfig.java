package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CategoryConfig {

    /**
     * The minimum level that this category can be set to
     */
    @ConfigItem(defaultValue = "inherit")
    String minLevel;

    /**
     * The log level level for this category
     */
    @ConfigItem(defaultValue = "inherit")
    String level;
}
