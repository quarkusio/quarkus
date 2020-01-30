package io.quarkus.runtime.logging;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class LogConfig {

    /**
     * The default log level
     */
    @ConfigItem
    public Optional<Level> level;

    /**
     * The default minimum log level
     */
    @ConfigItem(defaultValue = "INFO")
    public Level minLevel;

    /**
     * Logging categories.
     * <p>
     * Logging is done on a per-category basis. Each category can be independently configured.
     * A configuration which applies to a category will also apply to all sub-categories of that category,
     * unless there is a more specific matching sub-category configuration.
     */
    @ConfigItem(name = "category")
    @ConfigDocSection
    public Map<String, CategoryConfig> categories;

    /**
     * Console handlers.
     * <p>
     * The named console handlers configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.console")
    @ConfigDocSection
    public Map<String, ConsoleConfig> consoleHandlers;

    /**
     * File handlers.
     * <p>
     * The named file handlers configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.file")
    @ConfigDocSection
    public Map<String, FileConfig> fileHandlers;

    /**
     * Syslog handlers.
     * <p>
     * The named syslog handlers configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.syslog")
    @ConfigDocSection
    public Map<String, SyslogConfig> syslogHandlers;

    /**
     * Console logging.
     * <p>
     * Console logging is enabled by default.
     */
    @ConfigDocSection
    public ConsoleConfig console;

    /**
     * File logging.
     * <p>
     * Logging to a file is also supported but not enabled by default.
     */
    @ConfigDocSection
    public FileConfig file;

    /**
     * Syslog logging.
     * <p>
     * Logging to a syslog is also supported but not enabled by default.
     */
    @ConfigDocSection
    public SyslogConfig syslog;

    /**
     * Log cleanup filters - internal use.
     */
    @ConfigItem(name = "filter")
    @ConfigDocSection
    public Map<String, CleanupFilterConfig> filters;
}
