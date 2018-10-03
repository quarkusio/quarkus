/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2010 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.logging;

/**
 * An interface which specifies the basic logger methods.  When used as the base interface of a typed logger, these methods will delegate
 * to the corresponding underlying logger instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface BasicLogger {

    /**
     * Check to see if the given level is enabled for this logger.
     *
     * @param level the level to check for
     * @return {@code true} if messages may be logged at the given level, {@code false} otherwise
     */
    boolean isEnabled(Logger.Level level);

    /**
     * Check to see if the {@code TRACE} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link org.jboss.logging.Logger.Level#TRACE} may be accepted, {@code false} otherwise
     */
    boolean isTraceEnabled();

    /**
     * Issue a log message with a level of TRACE.
     *
     * @param message the message
     */
    void trace(Object message);

    /**
     * Issue a log message and throwable with a level of TRACE.
     *
     * @param message the message
     * @param t the throwable
     */
    void trace(Object message, Throwable t);

    /**
     * Issue a log message and throwable with a level of TRACE and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void trace(String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable with a level of TRACE.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void trace(String loggerFqcn, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    void tracev(String format, Object... params);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void tracev(String format, Object param1);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void tracev(String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void tracev(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void tracev(Throwable t, String format, Object... params);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void tracev(Throwable t, String format, Object param1);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void tracev(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void tracev(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void tracef(String format, Object... params);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void tracef(String format, Object param1);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void tracef(String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void tracef(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void tracef(Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void tracef(Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void tracef(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void tracef(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void tracef(String format, int arg);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(String format, int arg1, int arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(String format, int arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(String format, int arg1, int arg2, int arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(String format, int arg1, int arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(String format, int arg1, Object arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void tracef(Throwable t, String format, int arg);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(Throwable t, String format, int arg1, int arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(Throwable t, String format, int arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(Throwable t, String format, int arg1, int arg2, int arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(Throwable t, String format, int arg1, int arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(Throwable t, String format, int arg1, Object arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void tracef(String format, long arg);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(String format, long arg1, long arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(String format, long arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(String format, long arg1, long arg2, long arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(String format, long arg1, long arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(String format, long arg1, Object arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void tracef(Throwable t, String format, long arg);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(Throwable t, String format, long arg1, long arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void tracef(Throwable t, String format, long arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(Throwable t, String format, long arg1, long arg2, long arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(Throwable t, String format, long arg1, long arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void tracef(Throwable t, String format, long arg1, Object arg2, Object arg3);

    /**
     * Check to see if the {@code DEBUG} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link org.jboss.logging.Logger.Level#DEBUG} may be accepted, {@code false} otherwise
     */
    boolean isDebugEnabled();

    /**
     * Issue a log message with a level of DEBUG.
     *
     * @param message the message
     */
    void debug(Object message);

    /**
     * Issue a log message and throwable with a level of DEBUG.
     *
     * @param message the message
     * @param t the throwable
     */
    void debug(Object message, Throwable t);

    /**
     * Issue a log message and throwable with a level of DEBUG and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void debug(String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable with a level of DEBUG.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void debug(String loggerFqcn, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    void debugv(String format, Object... params);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void debugv(String format, Object param1);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void debugv(String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void debugv(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void debugv(Throwable t, String format, Object... params);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void debugv(Throwable t, String format, Object param1);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void debugv(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void debugv(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void debugf(String format, Object... params);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void debugf(String format, Object param1);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void debugf(String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void debugf(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void debugf(Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void debugf(Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void debugf(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void debugf(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void debugf(String format, int arg);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(String format, int arg1, int arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(String format, int arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(String format, int arg1, int arg2, int arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(String format, int arg1, int arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(String format, int arg1, Object arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void debugf(Throwable t, String format, int arg);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(Throwable t, String format, int arg1, int arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(Throwable t, String format, int arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(Throwable t, String format, int arg1, int arg2, int arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(Throwable t, String format, int arg1, int arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(Throwable t, String format, int arg1, Object arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void debugf(String format, long arg);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(String format, long arg1, long arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(String format, long arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(String format, long arg1, long arg2, long arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(String format, long arg1, long arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(String format, long arg1, Object arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg the parameter
     */
    void debugf(Throwable t, String format, long arg);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(Throwable t, String format, long arg1, long arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     */
    void debugf(Throwable t, String format, long arg1, Object arg2);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(Throwable t, String format, long arg1, long arg2, long arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(Throwable t, String format, long arg1, long arg2, Object arg3);

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param arg1 the first parameter
     * @param arg2 the second parameter
     * @param arg3 the third parameter
     */
    void debugf(Throwable t, String format, long arg1, Object arg2, Object arg3);

    /**
     * Check to see if the {@code INFO} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link org.jboss.logging.Logger.Level#INFO} may be accepted, {@code false} otherwise
     */
    boolean isInfoEnabled();

    /**
     * Issue a log message with a level of INFO.
     *
     * @param message the message
     */
    void info(Object message);

    /**
     * Issue a log message and throwable with a level of INFO.
     *
     * @param message the message
     * @param t the throwable
     */
    void info(Object message, Throwable t);

    /**
     * Issue a log message and throwable with a level of INFO and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void info(String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable with a level of INFO.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void info(String loggerFqcn, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    void infov(String format, Object... params);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void infov(String format, Object param1);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void infov(String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void infov(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void infov(Throwable t, String format, Object... params);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void infov(Throwable t, String format, Object param1);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void infov(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void infov(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void infof(String format, Object... params);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void infof(String format, Object param1);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void infof(String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void infof(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void infof(Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void infof(Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void infof(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void infof(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of WARN.
     *
     * @param message the message
     */
    void warn(Object message);

    /**
     * Issue a log message and throwable with a level of WARN.
     *
     * @param message the message
     * @param t the throwable
     */
    void warn(Object message, Throwable t);

    /**
     * Issue a log message and throwable with a level of WARN and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void warn(String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable with a level of WARN.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void warn(String loggerFqcn, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    void warnv(String format, Object... params);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void warnv(String format, Object param1);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void warnv(String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void warnv(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void warnv(Throwable t, String format, Object... params);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void warnv(Throwable t, String format, Object param1);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void warnv(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void warnv(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void warnf(String format, Object... params);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void warnf(String format, Object param1);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void warnf(String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void warnf(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void warnf(Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void warnf(Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void warnf(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void warnf(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of ERROR.
     *
     * @param message the message
     */
    void error(Object message);

    /**
     * Issue a log message and throwable with a level of ERROR.
     *
     * @param message the message
     * @param t the throwable
     */
    void error(Object message, Throwable t);

    /**
     * Issue a log message and throwable with a level of ERROR and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void error(String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable with a level of ERROR.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void error(String loggerFqcn, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    void errorv(String format, Object... params);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void errorv(String format, Object param1);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void errorv(String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void errorv(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void errorv(Throwable t, String format, Object... params);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void errorv(Throwable t, String format, Object param1);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void errorv(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void errorv(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void errorf(String format, Object... params);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void errorf(String format, Object param1);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void errorf(String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void errorf(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void errorf(Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void errorf(Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void errorf(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void errorf(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of FATAL.
     *
     * @param message the message
     */
    void fatal(Object message);

    /**
     * Issue a log message and throwable with a level of FATAL.
     *
     * @param message the message
     * @param t the throwable
     */
    void fatal(Object message, Throwable t);

    /**
     * Issue a log message and throwable with a level of FATAL and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void fatal(String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable with a level of FATAL.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void fatal(String loggerFqcn, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    void fatalv(String format, Object... params);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void fatalv(String format, Object param1);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void fatalv(String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void fatalv(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void fatalv(Throwable t, String format, Object... params);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void fatalv(Throwable t, String format, Object param1);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void fatalv(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void fatalv(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void fatalf(String format, Object... params);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void fatalf(String format, Object param1);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void fatalf(String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void fatalf(String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void fatalf(Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void fatalf(Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void fatalf(Throwable t, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void fatalf(Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Log a message at the given level.
     *
     * @param level the level
     * @param message the message
     */
    void log(Logger.Level level, Object message);

    /**
     * Issue a log message and throwable at the given log level.
     *
     * @param level the level
     * @param message the message
     * @param t the throwable
     */
    void log(Logger.Level level, Object message, Throwable t);

    /**
     * Issue a log message and throwable at the given log level and a specific logger class name.
     *
     * @param level the level
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    void log(Logger.Level level, String loggerFqcn, Object message, Throwable t);

    /**
     * Issue a log message with parameters and a throwable at the given log level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    void log(String loggerFqcn, Logger.Level level, Object message, Object[] params, Throwable t);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param params the parameters
     */
    void logv(Logger.Level level, String format, Object... params);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void logv(Logger.Level level, String format, Object param1);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void logv(Logger.Level level, String format, Object param1, Object param2);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void logv(Logger.Level level, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void logv(Logger.Level level, Throwable t, String format, Object... params);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void logv(Logger.Level level, Throwable t, String format, Object param1);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void logv(Logger.Level level, Throwable t, String format, Object param1, Object param2);

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
    void logv(Logger.Level level, Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object... params);

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1);

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
    void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2);

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
    void logv(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    void logf(Logger.Level level, String format, Object... params);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void logf(Logger.Level level, String format, Object param1);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void logf(Logger.Level level, String format, Object param1, Object param2);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    void logf(Logger.Level level, String format, Object param1, Object param2, Object param3);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    void logf(Logger.Level level, Throwable t, String format, Object... params);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    void logf(Logger.Level level, Throwable t, String format, Object param1);

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    void logf(Logger.Level level, Throwable t, String format, Object param1, Object param2);

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
    void logf(Logger.Level level, Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Log a message at the given level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable cause
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1);

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
    void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2);

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
    void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object param1, Object param2, Object param3);

    /**
     * Log a message at the given level.
     *
     * @param loggerFqcn the logger class name
     * @param level the level
     * @param t the throwable cause
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the message parameters
     */
    void logf(String loggerFqcn, Logger.Level level, Throwable t, String format, Object... params);
}
