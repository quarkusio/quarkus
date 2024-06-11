package io.quarkus.websockets.next;

/**
 * The strategy used when an error occurs but no error handler can handle the failure.
 */
public enum UnhandledFailureStrategy {
    /**
     * Close the connection.
     */
    CLOSE,
    /**
     * Log an error message.
     */
    LOG,
    /**
     * No operation.
     */
    NOOP;

}