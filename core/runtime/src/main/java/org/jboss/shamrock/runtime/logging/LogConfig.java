package org.jboss.shamrock.runtime.logging;

import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

import org.jboss.shamrock.runtime.annotations.ConfigItem;
import org.jboss.shamrock.runtime.annotations.ConfigPhase;
import org.jboss.shamrock.runtime.annotations.ConfigRoot;

/**
 *
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public final class LogConfig {
    /**
     * The log category config
     */
    @ConfigItem(name = "category")
    public Map<String, CategoryConfig> categories;

    /**
     * The log cleanup filter config
     */
    @ConfigItem(name = "filter")
    public Map<String, CleanupFilterConfig> filters;

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
     * Console logging config
     */
    public ConsoleConfig console;

    /**
     * File logging config
     */
    public FileConfig file;
}
