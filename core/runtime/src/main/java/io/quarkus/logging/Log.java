package io.quarkus.logging;

import org.jboss.logging.Logger;

/**
 * Copy of {@link org.jboss.logging.BasicLogger}.
 * Invocations of all {@code static} methods of this class are, during build time, replaced by invocations
 * of the same methods on a generated instance of {@link Logger}.
 */
public final class Log {

    /**
     * Check to see if the given level is enabled for this logger.
     *
     * @param level the level to check for
     * @return {@code true} if messages may be logged at the given level, {@code false} otherwise
     */
    public static boolean isEnabled(Logger.Level level) {
        throw fail();
    }

    /**
     * Check to see if the {@code TRACE} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link org.jboss.logging.Logger.Level#TRACE} may be accepted, {@code false}
     *         otherwise
     */
    public static boolean isTraceEnabled() {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE.
     *
     * @param message the message
     */
    public static void trace(Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of TRACE.
     *
     * @param message the message
     * @param t the throwable
     */
    public static void trace(Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of TRACE and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void trace(String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable with a level of TRACE.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void trace(String loggerFqcn, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public static void tracev(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void tracev(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void tracev(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void tracev(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void tracev(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void tracev(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void tracev(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void tracev(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void tracef(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void tracef(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void tracef(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void tracef(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void tracef(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void tracef(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void tracef(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void tracef(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void tracef(String format, int arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(String format, int arg1, int arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(String format, int arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(String format, int arg1, int arg2, int arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(String format, int arg1, int arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(String format, int arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void tracef(Throwable t, String format, int arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(Throwable t, String format, int arg1, int arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(Throwable t, String format, int arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(Throwable t, String format, int arg1, int arg2, int arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(Throwable t, String format, int arg1, int arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(Throwable t, String format, int arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void tracef(String format, long arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(String format, long arg1, long arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(String format, long arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(String format, long arg1, long arg2, long arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(String format, long arg1, long arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(String format, long arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void tracef(Throwable t, String format, long arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(Throwable t, String format, long arg1, long arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void tracef(Throwable t, String format, long arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(Throwable t, String format, long arg1, long arg2, long arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(Throwable t, String format, long arg1, long arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void tracef(Throwable t, String format, long arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Check to see if the {@code DEBUG} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link org.jboss.logging.Logger.Level#DEBUG} may be accepted, {@code false}
     *         otherwise
     */
    public static boolean isDebugEnabled() {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG.
     *
     * @param message the message
     */
    public static void debug(Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of DEBUG.
     *
     * @param message the message
     * @param t the throwable
     */
    public static void debug(Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of DEBUG and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void debug(String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable with a level of DEBUG.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void debug(String loggerFqcn, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public static void debugv(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void debugv(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void debugv(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void debugv(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void debugv(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void debugv(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void debugv(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void debugv(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void debugf(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void debugf(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void debugf(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void debugf(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void debugf(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void debugf(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void debugf(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void debugf(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void debugf(String format, int arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(String format, int arg1, int arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(String format, int arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(String format, int arg1, int arg2, int arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(String format, int arg1, int arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(String format, int arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void debugf(Throwable t, String format, int arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(Throwable t, String format, int arg1, int arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(Throwable t, String format, int arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(Throwable t, String format, int arg1, int arg2, int arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(Throwable t, String format, int arg1, int arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(Throwable t, String format, int arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void debugf(String format, long arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(String format, long arg1, long arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(String format, long arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(String format, long arg1, long arg2, long arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(String format, long arg1, long arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(String format, long arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    public static void debugf(Throwable t, String format, long arg) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(Throwable t, String format, long arg1, long arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    public static void debugf(Throwable t, String format, long arg1, Object arg2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(Throwable t, String format, long arg1, long arg2, long arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(Throwable t, String format, long arg1, long arg2, Object arg3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    public static void debugf(Throwable t, String format, long arg1, Object arg2, Object arg3) {
        throw fail();
    }

    /**
     * Check to see if the {@code INFO} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link org.jboss.logging.Logger.Level#INFO} may be accepted, {@code false}
     *         otherwise
     */
    public static boolean isInfoEnabled() {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO.
     *
     * @param message the message
     */
    public static void info(Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of INFO.
     *
     * @param message the message
     * @param t the throwable
     */
    public static void info(Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of INFO and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void info(String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable with a level of INFO.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void info(String loggerFqcn, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public static void infov(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void infov(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void infov(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void infov(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void infov(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void infov(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void infov(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void infov(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void infof(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void infof(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void infof(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void infof(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void infof(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void infof(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void infof(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void infof(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN.
     *
     * @param message the message
     */
    public static void warn(Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of WARN.
     *
     * @param message the message
     * @param t the throwable
     */
    public static void warn(Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of WARN and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void warn(String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable with a level of WARN.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void warn(String loggerFqcn, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public static void warnv(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void warnv(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void warnv(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void warnv(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void warnv(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void warnv(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void warnv(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void warnv(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void warnf(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void warnf(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void warnf(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void warnf(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void warnf(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void warnf(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void warnf(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void warnf(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR.
     *
     * @param message the message
     */
    public static void error(Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of ERROR.
     *
     * @param message the message
     * @param t the throwable
     */
    public static void error(Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of ERROR and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void error(String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable with a level of ERROR.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void error(String loggerFqcn, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public static void errorv(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void errorv(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void errorv(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void errorv(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void errorv(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void errorv(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void errorv(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void errorv(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void errorf(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void errorf(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void errorf(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void errorf(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void errorf(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void errorf(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void errorf(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void errorf(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL.
     *
     * @param message the message
     */
    public static void fatal(Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of FATAL.
     *
     * @param message the message
     * @param t the throwable
     */
    public static void fatal(Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable with a level of FATAL and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void fatal(String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable with a level of FATAL.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void fatal(String loggerFqcn, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public static void fatalv(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void fatalv(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void fatalv(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void fatalv(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void fatalv(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void fatalv(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void fatalv(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void fatalv(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void fatalf(String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void fatalf(String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void fatalf(String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void fatalf(String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void fatalf(Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void fatalf(Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void fatalf(Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void fatalf(Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Log a message at the given level.
     *
     * @param level the level
     * @param message the message
     */
    public static void log(Logger.Level level, Object message) {
        throw fail();
    }

    /**
     * Issue a log message and throwable at the given log level.
     *
     * @param level the level
     * @param message the message
     * @param t the throwable
     */
    public static void log(Logger.Level level, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message and throwable at the given log level and a specific logger class name.
     *
     * @param level the level
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public static void log(Logger.Level level, String loggerFqcn, Object message, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message with parameters and a throwable at the given log level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public static void log(String loggerFqcn, Logger.Level level, Object message, Object[] params, Throwable t) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param params the parameters
     */
    public static void logv(Logger.Level level, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void logv(Logger.Level level, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void logv(Logger.Level level, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void logv(Logger.Level level, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void logv(Logger.Level level, Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void logv(Logger.Level level, Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void logv(Logger.Level level, Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void logv(Logger.Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public static void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public static void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2,
            Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public static void logf(Logger.Level level, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void logf(Logger.Level level, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void logf(Logger.Level level, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void logf(Logger.Level level, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public static void logf(Logger.Level level, Throwable t, String format, Object... params) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public static void logf(Logger.Level level, Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void logf(Logger.Level level, Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void logf(Logger.Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
        throw fail();
    }

    /**
     * Log a message at the given level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable cause
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public static void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1) {
        throw fail();
    }

    /**
     * Log a message at the given level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable cause
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public static void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2) {
        throw fail();
    }

    /**
     * Log a message at the given level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable cause
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public static void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2,
            Object param3) {
        throw fail();
    }

    /**
     * Log a message at the given level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable cause
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the message parameters
     */
    public static void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object... params) {
        throw fail();
    }

    private static UnsupportedOperationException fail() {
        return new UnsupportedOperationException("Using " + Log.class.getName()
                + " is only possible with Quarkus bytecode transformation");
    }
}
