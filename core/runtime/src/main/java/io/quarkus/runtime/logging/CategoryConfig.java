package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CategoryConfig {

    /**
     * The log level level for this category
     */
    @ConfigItem(defaultValue = "inherit")
    String level;
}
