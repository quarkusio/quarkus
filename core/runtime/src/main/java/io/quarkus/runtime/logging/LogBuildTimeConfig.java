package io.quarkus.runtime.logging;

import java.util.Map;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot(name = "log", phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public class LogBuildTimeConfig {

    /**
     * Whether or not logging metrics are published in case a metrics extension is present.
     */
    @ConfigItem(name = "metrics.enabled", defaultValue = "false")
    public boolean metricsEnabled;

    /**
     * The default minimum log level.
     */
    @ConfigItem(defaultValue = "DEBUG")
    public Level minLevel;

    /**
     * Minimum logging categories.
     * <p>
     * Logging is done on a per-category basis. Each category can be independently configured.
     * A configuration which applies to a category will also apply to all sub-categories of that category,
     * unless there is a more specific matching sub-category configuration.
     */
    @ConfigItem(name = "category")
    @ConfigDocSection
    public Map<String, CategoryBuildTimeConfig> categories;

    /**
     * If this is true then in development mode classes from the current application will be underlined
     * in stack traces, to make it easier to differentiate between framework and application code.
     *
     * This only applies to the console log.
     */
    @ConfigItem(defaultValue = "true")
    public boolean highlightAppCode;

    /**
     * If this is true then in development and test mode stack traces will be trimmed to start at the point
     * that application code starts executing, to reduce noise.
     *
     * This only applies to the console log.
     */
    @ConfigItem(defaultValue = "true")
    public boolean trimStackTraces;
}
