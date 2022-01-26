package io.quarkus.logging.json.runtime;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for JSON log formatting.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log")
public class JsonLogConfig {
    /**
     * Console logging configuration
     */
    @ConfigItem(name = "console.json")
    JsonConfig console;
    /**
     * File logging configuration
     */
    @ConfigItem(name = "file.json")
    JsonConfig file;
    /**
     * Syslog logging configuration
     */
    @ConfigItem(name = "syslog.json")
    JsonConfig syslog;
    /**
     * Socket logging configuration
     */
    @ConfigItem(name = "socket.json")
    JsonConfig socket;

    /**
     * Named console handler formatters
     * <p>
     * The named console formatters configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.console")
    @ConfigDocSection
    public Map<String, NamedHandlerJsonConfig> consoleFormatters;

    /**
     * Named file handler formatters
     * <p>
     * The named file formatters configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.file")
    @ConfigDocSection
    public Map<String, NamedHandlerJsonConfig> fileFormatters;

    /**
     * Named syslog handler formatters
     * <p>
     * The named syslog formatters configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.syslog")
    @ConfigDocSection
    public Map<String, NamedHandlerJsonConfig> syslogFormatters;

    /**
     * Named syslog handler formatters
     * <p>
     * The named syslog formatters configured here can be linked on one or more categories.
     */
    @ConfigItem(name = "handler.socket")
    @ConfigDocSection
    public Map<String, NamedHandlerJsonConfig> socketFormatters;
}
