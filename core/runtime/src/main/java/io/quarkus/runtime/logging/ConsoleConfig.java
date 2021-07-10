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
     * The log format. Note that this value will be ignored if an extension is present that takes
     * control of console formatting (e.g. an XML or JSON-format extension).
     */
    @ConfigItem(defaultValue = "%d{yyyy-MM-dd HH:mm:ss,SSS} %-5p [%c{3.}] (%t) %s%e%n")
    String format;

    /**
     * The console log level.
     */
    @ConfigItem(defaultValue = "ALL")
    Level level;

    /**
     * If the console logging should be in color. If undefined quarkus takes
     * best guess based on operating system and environment.
     * Note that this value will be ignored if an extension is present that takes
     * control of console formatting (e.g. an XML or JSON-format extension).
     *
     * This has been deprecated, and replaced with quarkus.console.color instead,
     * as quarkus now provides more console based functionality than just logging.
     */
    @ConfigItem
    @Deprecated
    public Optional<Boolean> color;

    /**
     * Specify how much the colors should be darkened.
     * Note that this value will be ignored if an extension is present that takes
     * control of console formatting (e.g. an XML or JSON-format extension).
     */
    @ConfigItem(defaultValue = "0")
    int darken;

    /**
     * Console async logging config
     */
    AsyncConfig async;
}
