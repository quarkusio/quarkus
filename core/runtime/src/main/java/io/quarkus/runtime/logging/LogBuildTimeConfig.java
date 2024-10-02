package io.quarkus.runtime.logging;

import java.util.Map;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Logging
 */
@ConfigRoot(name = "log", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LogBuildTimeConfig {

    /**
     * If enabled and a metrics extension is present, logging metrics are published.
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    public boolean metricsEnabled;

    /**
     * The default minimum log level.
     */
    @ConfigItem(defaultValue = "DEBUG")
    public Level minLevel;

    /**
     * This will decorate the stacktrace in dev mode to show the line in the code that cause the exception
     */
    @ConfigItem(defaultValue = "true")
    public Boolean decorateStacktraces;

    /**
     * Minimum logging categories.
     * <p>
     * Logging is done on a per-category basis. Each category can be configured independently.
     * A configuration that applies to a category will also apply to all sub-categories of that category,
     * unless there is a more specific matching sub-category configuration.
     */
    @ConfigItem(name = "category")
    @ConfigDocSection
    public Map<String, CategoryBuildTimeConfig> categories;
}
