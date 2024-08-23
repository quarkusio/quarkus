package io.quarkus.runtime.logging;

import java.util.Optional;
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
     * If console logging should go to {@link System#err} instead of {@link System#out}.
     */
    @ConfigItem(defaultValue = "false")
    boolean stderr;

    /**
     * The log format. Note that this value is ignored if an extension is present that takes
     * control of console formatting (e.g., an XML or JSON-format extension).
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The console log level.
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * If the console logging should be in color. If undefined, Quarkus takes
     * best guess based on the operating system and environment.
     * Note that this value is ignored if an extension is present that takes
     * control of console formatting (e.g., an XML or JSON-format extension).
     * <p>
     * This has been deprecated and replaced with <code>quarkus.console.color</code>,
     * as Quarkus now provides more console-based functionality than just logging.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> color;

    /**
     * Specify how much the colors should be darkened.
     * Note that this value is ignored if an extension is present that takes
     * control of console formatting (e.g., an XML or JSON-format extension).
     */
    @ConfigItem(defaultValue = "0")
    int darken;

    /**
     * The name of the filter to link to the console handler.
     */
    @ConfigItem
    Optional<String> filter;

    /**
     * Console async logging config
     */
    AsyncConfig async;
}
