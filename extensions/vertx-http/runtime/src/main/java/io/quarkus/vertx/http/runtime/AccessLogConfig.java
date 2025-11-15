package io.quarkus.vertx.http.runtime;

import java.util.Optional;
import java.util.Set;

import io.smallrye.config.WithDefault;

public interface AccessLogConfig {
    /**
     * If access logging is enabled. By default this will log via the standard logging facility
     */
    @WithDefault("false")
    boolean enabled();

    /**
     * A regular expression that can be used to exclude some paths from logging.
     */
    Optional<String> excludePattern();

    /**
     * The access log pattern.
     * <p>
     * If this is the string `common`, `combined` or `long` then this will use one of the specified named formats:
     * <p>
     * - common: `%h %l %u %t "%r" %s %b`
     * - combined: `%h %l %u %t "%r" %s %b "%{i,Referer}" "%{i,User-Agent}"`
     * - long: `%r\n%{ALL_REQUEST_HEADERS}`
     * <p>
     * Otherwise, consult the Quarkus documentation for the full list of variables that can be used.
     *
     * Note that enabling the `%{ALL_REQUEST_HEADERS}` attribute directly or with a `long` named format introduces a risk
     * of sensitive header values being logged.
     * <p>
     * HTTP `Authorization` header value is always masked. Use the {@link #maskedHeaders()} property to mask other sensitive
     * headers.
     */
    @WithDefault("common")
    String pattern();

    /**
     * Set of HTTP headers whose values must be masked when the `%{ALL_REQUEST_HEADERS}` attribute
     * is enabled with the {@link #pattern()} property.
     */
    Optional<Set<String>> maskedHeaders();

    /**
     * Set of HTTP Cookie headers whose values must be masked when the `%{ALL_REQUEST_HEADERS}` attribute
     * is enabled with the {@link #pattern()} property.
     */
    Optional<Set<String>> maskedCookies();

    /**
     * If logging should be done to a separate file.
     */
    @WithDefault("false")
    boolean logToFile();

    /**
     * The access log file base name, defaults to 'quarkus' which will give a log file
     * name of 'quarkus.log'.
     *
     */
    @WithDefault("quarkus")
    String baseFileName();

    /**
     * The log directory to use when logging access to a file
     * <p>
     * If this is not set then the current working directory is used.
     */
    Optional<String> logDirectory();

    /**
     * The log file suffix
     */
    @WithDefault(".log")
    String logSuffix();

    /**
     * The log category to use if logging is being done via the standard log mechanism (i.e. if base-file-name is empty).
     *
     */
    @WithDefault("io.quarkus.http.access-log")
    String category();

    /**
     * If the log should be rotated daily
     */
    @WithDefault("true")
    boolean rotate();

    /**
     * If rerouted requests should be consolidated into one log entry
     */
    @WithDefault("false")
    boolean consolidateReroutedRequests();
}
