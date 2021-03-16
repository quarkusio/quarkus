package io.quarkus.runtime.logging;

import java.util.List;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CategoryConfig {

    /**
     * The log level for this category.
     *
     * Note that to get log levels below <code>INFO</code>,
     * the minimum level build time configuration option needs to be adjusted as well.
     */
    @ConfigItem(defaultValue = "inherit")
    InheritableLevel level;

    /**
     * The names of the handlers to link to this category.
     */
    @ConfigItem
    Optional<List<String>> handlers;

    /**
     * Specify whether or not this logger should send its output to its parent Logger
     */
    @ConfigItem(defaultValue = "true")
    boolean useParentHandlers;

}
