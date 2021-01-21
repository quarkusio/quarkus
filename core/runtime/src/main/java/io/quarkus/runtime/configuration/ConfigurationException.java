package io.quarkus.runtime.configuration;

import java.util.Collections;
import java.util.Set;

import io.quarkus.dev.config.ConfigurationProblem;

/**
 * An exception indicating that a configuration failure has occurred.
 */
public class ConfigurationException extends RuntimeException implements ConfigurationProblem {
    private static final long serialVersionUID = 4445679764085720090L;

    private final Set<String> configKeys;

    /**
     * Constructs a new {@code ConfigurationException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     * 
     * @param configKeys
     */
    public ConfigurationException(Set<String> configKeys) {
        this.configKeys = configKeys;
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public ConfigurationException(final String msg) {
        this(msg, Collections.emptySet());
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     * @param configKeys
     */
    public ConfigurationException(final String msg, Set<String> configKeys) {
        super(msg);
        this.configKeys = configKeys;
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code ConfigurationException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     * @param configKeys
     */
    public ConfigurationException(final Throwable cause, Set<String> configKeys) {
        super(cause);
        this.configKeys = configKeys;
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public ConfigurationException(final String msg, final Throwable cause) {
        this(msg, cause, Collections.emptySet());
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial message and cause.
     * 
     * @param msg the message
     * @param cause the cause
     * @param configKeys
     */
    public ConfigurationException(final String msg, final Throwable cause, Set<String> configKeys) {
        super(msg, cause);
        this.configKeys = configKeys;
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
        configKeys = Collections.emptySet();
    }

    public Set<String> getConfigKeys() {
        return configKeys;
    }
}
