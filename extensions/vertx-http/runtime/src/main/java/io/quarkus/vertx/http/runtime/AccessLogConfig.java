package io.quarkus.vertx.http.runtime;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class AccessLogConfig {

    /**
     * If access logging is enabled. By default this will log via the standard logging facility
     */
    @ConfigItem(defaultValue = "false")
    public boolean enabled;

    /**
     * A regular expression that can be used to exclude some paths from logging.
     */
    @ConfigItem
    Optional<String> excludePattern;

    /**
     * The access log pattern.
     *
     * If this is the string `common`, `combined` or `long` then this will use one of the specified named formats:
     *
     * - common: `%h %l %u %t "%r" %s %b`
     * - combined: `%h %l %u %t "%r" %s %b "%{i,Referer}" "%{i,User-Agent}"`
     * - long: `%r\n%{ALL_REQUEST_HEADERS}`
     *
     * Otherwise consult the Quarkus documentation for the full list of variables that can be used.
     *
     * @asciidoclet
     */
    @ConfigItem(defaultValue = "common")
    public String pattern;

    /**
     * If logging should be done to a separate file.
     */
    @ConfigItem(defaultValue = "false")
    public boolean logToFile;

    /**
     * The access log file base name, defaults to 'quarkus' which will give a log file
     * name of 'quarkus.log'.
     *
     */
    @ConfigItem(defaultValue = "quarkus")
    public String baseFileName;

    /**
     * The log directory to use when logging access to a file
     *
     * If this is not set then the current working directory is used.
     */
    @ConfigItem
    public Optional<String> logDirectory;

    /**
     * The log file suffix
     */
    @ConfigItem(defaultValue = ".log")
    public String logSuffix;

    /**
     * The log category to use if logging is being done via the standard log mechanism (i.e. if base-file-name is empty).
     *
     */
    @ConfigItem(defaultValue = "io.quarkus.http.access-log")
    public String category;

    /**
     * If the log should be rotated daily
     */
    @ConfigItem(defaultValue = "true")
    public boolean rotate;

}
