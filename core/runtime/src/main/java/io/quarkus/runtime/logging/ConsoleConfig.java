package io.quarkus.runtime.logging;

import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class ConsoleConfig {

    /**
     * If console logging should be enabled
     */
    @ConfigItem(defaultValue = "true")
    boolean enable;

    /**
     * The log format
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The console log level
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * If the console logging should be in color
     */
    @ConfigItem(defaultValue = "true")
    boolean color;

    /**
     * Specify how much the colors should be darkened
     */
    @ConfigItem(defaultValue = "0")
    int darken;

    /**
     * Console async logging config
     */
    AsyncConfig async;
}
