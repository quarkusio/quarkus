package io.quarkus.runtime.configuration;

/**
 * An exception indicating that a configuration failure has occurred.
 */
public class ConfigurationException extends RuntimeException {
    private static final long serialVersionUID = 4445679764085720090L;

    /**
     * Constructs a new {@code ConfigurationException} instance. The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public ConfigurationException() {
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial message. No
     * cause is specified.
     *
     * @param msg the message
     */
    public ConfigurationException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial cause. If
     * a non-{@code null} cause is specified, its message is used to initialize the message of this
     * {@code ConfigurationException}; otherwise the message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code ConfigurationException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public ConfigurationException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
