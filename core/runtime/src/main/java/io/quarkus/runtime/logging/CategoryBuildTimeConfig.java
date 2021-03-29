package io.quarkus.runtime.logging;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class CategoryBuildTimeConfig {
    /**
     * The minimum log level for this category.
     * By default all categories are configured with <code>DEBUG</code> minimum level.
     *
     * To get runtime logging below <code>DEBUG</code>, e.g. <code>TRACE</code>,
     * the minimum level has to be adjusted at build time, the right log level needs to be provided at runtime.
     *
     * As an example, to get <code>TRACE</code> logging,
     * minimum level needs to be at <code>TRACE</code> and the runtime log level needs to match that.
     */
    @ConfigItem(defaultValue = "inherit")
    public InheritableLevel minLevel;
}
