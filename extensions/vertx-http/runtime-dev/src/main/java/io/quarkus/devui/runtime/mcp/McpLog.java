package io.quarkus.devui.runtime.mcp;

import org.jboss.logging.Logger;

/**
 * Send log message notifications to a connected MCP client.
 * <p>
 * The MCP logger name is derived from the method. For example, if there is a method {@code myTool()} annotated with
 * {@code @Tool} then the logger name will be {@code tool:myTool}.
 */
interface McpLog {

    /**
     *
     * @return the current log level
     */
    LogLevel level();

    /**
     * Sends a log message notification to the client if the specified level is higher or equal to the current level.
     *
     * @param level
     * @param data
     */
    void send(LogLevel level, Object data);

    /**
     * Sends a log message notification to the client if the specified level is higher or equal to the current level.
     *
     * @param level
     * @param format
     * @param params
     */
    void send(LogLevel level, String format, Object... params);

    /**
     * Logs a message and sends a {@link LogLevel#DEBUG} log message notification to the client.
     *
     * @param format
     * @param params
     * @see Logger#debugf(String, Object...)
     */
    void debug(String format, Object... params);

    /**
     * Logs a message and sends a {@link LogLevel#INFO} log message notification to the client.
     *
     * @param format
     * @param params
     * @see Logger#infof(String, Object...)
     */
    void info(String format, Object... params);

    /**
     * Logs a message and sends a {@link LogLevel#ERROR} log message notification to the client.
     *
     * @param format
     * @param params
     * @see Logger#errorf(String, Object...)
     */
    void error(String format, Object... params);

    /**
     * Logs a message and sends a {@link LogLevel#ERROR} log message notification to the client.
     *
     * @param t
     * @param format
     * @param params
     * @see Logger#errorf(Throwable, String, Object...)
     */
    void error(Throwable t, String format, Object... params);

    enum LogLevel {
        DEBUG,
        INFO,
        NOTICE,
        WARNING,
        ERROR,
        CRITICAL,
        ALERT,
        EMERGENCY;

        public static LogLevel from(String val) {
            if (val == null || val.isBlank()) {
                return null;
            }
            try {
                return valueOf(val.toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }

    }

}
