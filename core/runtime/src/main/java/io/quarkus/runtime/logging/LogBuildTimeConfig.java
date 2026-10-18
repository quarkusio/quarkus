package io.quarkus.runtime.logging;

import java.util.Map;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Logging
 */
@ConfigMapping(prefix = "quarkus.log")
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface LogBuildTimeConfig {
    /**
     * If enabled and a metrics extension is present, logging metrics are published.
     */
    @WithName("metrics.enabled")
    @WithDefault("false")
    boolean metricsEnabled();

    /**
     * The default minimum log level.
     */
    @WithDefault("DEBUG")
    @WithConverter(LevelConverter.class)
    Level minLevel();

    /**
     * This will decorate the stacktrace in dev mode to show the line in the code that cause the exception
     */
    @WithDefault("true")
    boolean decorateStacktraces();

    /**
     * Minimum logging categories.
     * <p>
     * Logging is done on a per-category basis. Each category can be configured independently.
     * A configuration that applies to a category will also apply to all sub-categories of that category,
     * unless there is a more specific matching sub-category configuration.
     */
    @WithName("category")
    @ConfigDocSection
    Map<String, CategoryBuildTimeConfig> categories();

    interface CategoryBuildTimeConfig {
        /**
         * The minimum log level for this category.
         * By default, all categories are configured with <code>DEBUG</code> minimum level.
         * <p>
         * To get runtime logging below <code>DEBUG</code>, e.g., <code>TRACE</code>,
         * adjust the minimum level at build time. The right log level needs to be provided at runtime.
         * <p>
         * As an example, to get <code>TRACE</code> logging,
         * minimum level needs to be at <code>TRACE</code>, and the runtime log level needs to match that.
         */
        @WithDefault("inherit")
        InheritableLevel minLevel();
    }
}
