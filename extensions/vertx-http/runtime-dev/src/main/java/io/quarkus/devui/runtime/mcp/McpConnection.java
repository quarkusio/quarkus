package io.quarkus.devui.runtime.mcp;

/**
 * The connection from an MCP client.
 */
interface McpConnection {

    /**
     * @return the identifier (not {@code null})
     */
    String id();

    /**
     * @return the current status (not {@code null})
     */
    Status status();

    /**
     * @return the initial request (not {@code null})
     */
    InitialRequest initialRequest();

    /**
     * @return the current log level
     */
    McpLog.LogLevel logLevel();

    enum Status {

        /**
         * A new connnection, waiting for the {@code initialize} request from the client.
         */
        NEW,
        /**
         * The server responded to the {@code initialize} request with its own capabilities and information. Now it's waigting
         * for the {@code initialized} notification from the client.
         */
        INITIALIZING,
        /**
         * The client sent the {@code initialized} notification.
         */
        IN_OPERATION,
        /**
         * Connection was closed.
         */
        CLOSED
    }

}
