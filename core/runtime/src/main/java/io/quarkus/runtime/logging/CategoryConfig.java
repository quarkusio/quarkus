package io.quarkus.runtime.logging;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CategoryConfig {

    /**
     * The log level for this category.
     * <p>
     * Note that to get log levels below <code>INFO</code>,
     * the minimum level build-time configuration option also needs to be adjusted.
     */
    @ConfigItem(defaultValue = "inherit")
    InheritableLevel level;

    /**
     * The names of the handlers to link to this category.
     */
    @ConfigItem
    Optional<List<String>> handlers;

    /**
     * Specify whether this logger should send its output to its parent Logger
     */
    @ConfigItem(defaultValue = "true")
    boolean useParentHandlers;

    // for method refs
    public InheritableLevel getLevel() {
        return level;
    }
}
