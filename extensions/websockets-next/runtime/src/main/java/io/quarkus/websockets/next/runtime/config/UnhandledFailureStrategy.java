package io.quarkus.websockets.next.runtime.config;

/**
 * The strategy used when an error occurs but no error handler can handle the failure.
 */
public enum UnhandledFailureStrategy {
    /**
     * Log the error message and close the connection.
     */
    LOG_AND_CLOSE,
    /**
     * Close the connection silently.
     */
    CLOSE,
    /**
     * Log the error message.
     */
    LOG,
    /**
     * No operation.
     */
    NOOP;

}