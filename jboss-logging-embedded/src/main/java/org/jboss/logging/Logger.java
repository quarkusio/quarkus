/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2011 Red Hat, Inc.
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

import com.oracle.svm.core.annotate.AlwaysInline;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.util.Locale;

import static java.security.AccessController.doPrivileged;

/**
 * An abstracted logging entry point.
 */
public abstract class Logger implements Serializable, BasicLogger {

    private static final long serialVersionUID = 4232175575988879434L;

    private static final String FQCN = Logger.class.getName();

    /**
     * Levels used by this logging API.
     */
    public enum Level {
        FATAL,
        ERROR,
        WARN,
        INFO,
        DEBUG,
        TRACE,
    }

    private final String name;

    /**
     * Construct a new instance.
     *
     * @param name the logger category name
     */
    protected Logger(final String name) {
        this.name = name;
    }

    /**
     * Return the name of this logger.
     *
     * @return The name of this logger.
     */
    public String getName() {
        return name;
    }

    /**
     * Implementation log method (standard parameter formatting).
     *
     * @param level the level
     * @param loggerClassName the logger class name
     * @param message the message to log
     * @param parameters the parameters of the message
     * @param thrown the exception which was thrown, if any
     */
    protected abstract void doLog(Level level, String loggerClassName, Object message, Object[] parameters, Throwable thrown);

    /**
     * Implementation log method (printf formatting).
     *
     * @param level the level
     * @param loggerClassName the logger class name
     * @param format the format string to log
     * @param parameters the parameters of the message
     * @param thrown the exception which was thrown, if any
     */
    protected abstract void doLogf(Level level, String loggerClassName, String format, Object[] parameters, Throwable thrown);

    /**
     * Check to see if the {@code TRACE} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link Level#TRACE} may be accepted, {@code false} otherwise
     */
    @AlwaysInline("Fast level checks")
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    /**
     * Issue a log message with a level of TRACE.
     *
     * @param message the message
     */
    @AlwaysInline("Fast level checks")
    public void trace(Object message) {
        doLog(Level.TRACE, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable with a level of TRACE.
     *
     * @param message the message
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void trace(Object message, Throwable t) {
        doLog(Level.TRACE, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable with a level of TRACE and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void trace(String loggerFqcn, Object message, Throwable t) {
        doLog(Level.TRACE, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters with a level of TRACE.
     *
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #tracev(String, Object...)} is recommended.
     */
    @Deprecated
    @AlwaysInline("Fast level checks")
    public void trace(Object message, Object[] params) {
        doLog(Level.TRACE, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of TRACE.
     *
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #tracev(Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    @AlwaysInline("Fast level checks")
    public void trace(Object message, Object[] params, Throwable t) {
        doLog(Level.TRACE, FQCN, message, params, t);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of TRACE.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void trace(String loggerFqcn, Object message, Object[] params, Throwable t) {
        doLog(Level.TRACE, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void tracev(String format, Object... params) {
        doLog(Level.TRACE, FQCN, format, params, null);
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracev(String format, Object param1) {
        if (isEnabled(Level.TRACE)) {
            doLog(Level.TRACE, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracev(String format, Object param1, Object param2) {
        if (isEnabled(Level.TRACE)) {
            doLog(Level.TRACE, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracev(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.TRACE)) {
            doLog(Level.TRACE, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void tracev(Throwable t, String format, Object... params) {
        doLog(Level.TRACE, FQCN, format, params, t);
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracev(Throwable t, String format, Object param1) {
        if (isEnabled(Level.TRACE)) {
            doLog(Level.TRACE, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a log message with a level of TRACE using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracev(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.TRACE)) {
            doLog(Level.TRACE, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void tracev(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.TRACE)) {
            doLog(Level.TRACE, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void tracef(String format, Object... params) {
        doLogf(Level.TRACE, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracef(String format, Object param1) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracef(String format, Object param1, Object param2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracef(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void tracef(Throwable t, String format, Object... params) {
        doLogf(Level.TRACE, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracef(Throwable t, String format, Object param1) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of TRACE.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void tracef(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void tracef(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final int arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final Object arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final int arg2, final int arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final int arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final int arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final int arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final Object arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final int arg2, final int arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final int arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final int arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final long arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final Object arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final long arg2, final long arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final long arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final String format, final long arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final long arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final Object arg2) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final long arg2, final long arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final long arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void tracef(final Throwable t, final String format, final long arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.TRACE)) {
            doLogf(Level.TRACE, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    /**
     * Check to see if the {@code DEBUG} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link Level#DEBUG} may be accepted, {@code false} otherwise
     */
    @AlwaysInline("Fast level checks")
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    /**
     * Issue a log message with a level of DEBUG.
     *
     * @param message the message
     */
    @AlwaysInline("Fast level checks")
    public void debug(Object message) {
        doLog(Level.DEBUG, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable with a level of DEBUG.
     *
     * @param message the message
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void debug(Object message, Throwable t) {
        doLog(Level.DEBUG, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable with a level of DEBUG and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void debug(String loggerFqcn, Object message, Throwable t) {
        doLog(Level.DEBUG, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters with a level of DEBUG.
     *
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #debugv(String, Object...)} is recommended.
     */
    @Deprecated
    @AlwaysInline("Fast level checks")
    public void debug(Object message, Object[] params) {
        doLog(Level.DEBUG, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of DEBUG.
     *
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #debugv(Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    @AlwaysInline("Fast level checks")
    public void debug(Object message, Object[] params, Throwable t) {
        doLog(Level.DEBUG, FQCN, message, params, t);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of DEBUG.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void debug(String loggerFqcn, Object message, Object[] params, Throwable t) {
        doLog(Level.DEBUG, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void debugv(String format, Object... params) {
        doLog(Level.DEBUG, FQCN, format, params, null);
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugv(String format, Object param1) {
        if (isEnabled(Level.DEBUG)) {
            doLog(Level.DEBUG, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugv(String format, Object param1, Object param2) {
        if (isEnabled(Level.DEBUG)) {
            doLog(Level.DEBUG, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugv(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.DEBUG)) {
            doLog(Level.DEBUG, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void debugv(Throwable t, String format, Object... params) {
        doLog(Level.DEBUG, FQCN, format, params, t);
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugv(Throwable t, String format, Object param1) {
        if (isEnabled(Level.DEBUG)) {
            doLog(Level.DEBUG, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a log message with a level of DEBUG using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugv(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.DEBUG)) {
            doLog(Level.DEBUG, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void debugv(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.DEBUG)) {
            doLog(Level.DEBUG, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void debugf(String format, Object... params) {
        doLogf(Level.DEBUG, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugf(String format, Object param1) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugf(String format, Object param1, Object param2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugf(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void debugf(Throwable t, String format, Object... params) {
        doLogf(Level.DEBUG, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugf(Throwable t, String format, Object param1) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of DEBUG.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void debugf(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void debugf(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final int arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final Object arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final int arg2, final int arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final int arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final int arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final int arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final Object arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final int arg2, final int arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final int arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final int arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final long arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final Object arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final long arg2, final long arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final long arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final String format, final long arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, null);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final long arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final Object arg2) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final long arg2, final long arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final long arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    @AlwaysInline("Fast level checks")
    public void debugf(final Throwable t, final String format, final long arg1, final Object arg2, final Object arg3) {
        if (isEnabled(Level.DEBUG)) {
            doLogf(Level.DEBUG, FQCN, format, new Object[] { arg1, arg2, arg3 }, t);
        }
    }

    /**
     * Check to see if the {@code INFO} level is enabled for this logger.
     *
     * @return {@code true} if messages logged at {@link Level#INFO} may be accepted, {@code false} otherwise
     */
    @AlwaysInline("Fast level checks")
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    /**
     * Issue a log message with a level of INFO.
     *
     * @param message the message
     */
    public void info(Object message) {
        doLog(Level.INFO, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable with a level of INFO.
     *
     * @param message the message
     * @param t the throwable
     */
    public void info(Object message, Throwable t) {
        doLog(Level.INFO, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable with a level of INFO and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public void info(String loggerFqcn, Object message, Throwable t) {
        doLog(Level.INFO, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters with a level of INFO.
     *
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #infov(String, Object...)} is recommended.
     */
    @Deprecated
    public void info(Object message, Object[] params) {
        doLog(Level.INFO, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of INFO.
     *
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #infov(Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    public void info(Object message, Object[] params, Throwable t) {
        doLog(Level.INFO, FQCN, message, params, t);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of INFO.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public void info(String loggerFqcn, Object message, Object[] params, Throwable t) {
        doLog(Level.INFO, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public void infov(String format, Object... params) {
        doLog(Level.INFO, FQCN, format, params, null);
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void infov(String format, Object param1) {
        if (isEnabled(Level.INFO)) {
            doLog(Level.INFO, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void infov(String format, Object param1, Object param2) {
        if (isEnabled(Level.INFO)) {
            doLog(Level.INFO, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void infov(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.INFO)) {
            doLog(Level.INFO, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public void infov(Throwable t, String format, Object... params) {
        doLog(Level.INFO, FQCN, format, params, t);
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void infov(Throwable t, String format, Object param1) {
        if (isEnabled(Level.INFO)) {
            doLog(Level.INFO, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a log message with a level of INFO using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void infov(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.INFO)) {
            doLog(Level.INFO, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void infov(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.INFO)) {
            doLog(Level.INFO, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public void infof(String format, Object... params) {
        doLogf(Level.INFO, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public void infof(String format, Object param1) {
        if (isEnabled(Level.INFO)) {
            doLogf(Level.INFO, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void infof(String format, Object param1, Object param2) {
        if (isEnabled(Level.INFO)) {
            doLogf(Level.INFO, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void infof(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.INFO)) {
            doLogf(Level.INFO, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public void infof(Throwable t, String format, Object... params) {
        doLogf(Level.INFO, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public void infof(Throwable t, String format, Object param1) {
        if (isEnabled(Level.INFO)) {
            doLogf(Level.INFO, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of INFO.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void infof(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.INFO)) {
            doLogf(Level.INFO, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void infof(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.INFO)) {
            doLogf(Level.INFO, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a log message with a level of WARN.
     *
     * @param message the message
     */
    public void warn(Object message) {
        doLog(Level.WARN, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable with a level of WARN.
     *
     * @param message the message
     * @param t the throwable
     */
    public void warn(Object message, Throwable t) {
        doLog(Level.WARN, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable with a level of WARN and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public void warn(String loggerFqcn, Object message, Throwable t) {
        doLog(Level.WARN, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters with a level of WARN.
     *
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #warnv(String, Object...)} is recommended.
     */
    @Deprecated
    public void warn(Object message, Object[] params) {
        doLog(Level.WARN, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of WARN.
     *
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #warnv(Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    public void warn(Object message, Object[] params, Throwable t) {
        doLog(Level.WARN, FQCN, message, params, t);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of WARN.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public void warn(String loggerFqcn, Object message, Object[] params, Throwable t) {
        doLog(Level.WARN, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public void warnv(String format, Object... params) {
        doLog(Level.WARN, FQCN, format, params, null);
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void warnv(String format, Object param1) {
        if (isEnabled(Level.WARN)) {
            doLog(Level.WARN, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void warnv(String format, Object param1, Object param2) {
        if (isEnabled(Level.WARN)) {
            doLog(Level.WARN, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void warnv(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.WARN)) {
            doLog(Level.WARN, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public void warnv(Throwable t, String format, Object... params) {
        doLog(Level.WARN, FQCN, format, params, t);
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void warnv(Throwable t, String format, Object param1) {
        if (isEnabled(Level.WARN)) {
            doLog(Level.WARN, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a log message with a level of WARN using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void warnv(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.WARN)) {
            doLog(Level.WARN, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void warnv(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.WARN)) {
            doLog(Level.WARN, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public void warnf(String format, Object... params) {
        doLogf(Level.WARN, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public void warnf(String format, Object param1) {
        if (isEnabled(Level.WARN)) {
            doLogf(Level.WARN, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void warnf(String format, Object param1, Object param2) {
        if (isEnabled(Level.WARN)) {
            doLogf(Level.WARN, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void warnf(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.WARN)) {
            doLogf(Level.WARN, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public void warnf(Throwable t, String format, Object... params) {
        doLogf(Level.WARN, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public void warnf(Throwable t, String format, Object param1) {
        if (isEnabled(Level.WARN)) {
            doLogf(Level.WARN, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of WARN.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void warnf(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.WARN)) {
            doLogf(Level.WARN, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void warnf(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.WARN)) {
            doLogf(Level.WARN, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a log message with a level of ERROR.
     *
     * @param message the message
     */
    public void error(Object message) {
        doLog(Level.ERROR, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable with a level of ERROR.
     *
     * @param message the message
     * @param t the throwable
     */
    public void error(Object message, Throwable t) {
        doLog(Level.ERROR, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable with a level of ERROR and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public void error(String loggerFqcn, Object message, Throwable t) {
        doLog(Level.ERROR, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters with a level of ERROR.
     *
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #errorv(String, Object...)} is recommended.
     */
    @Deprecated
    public void error(Object message, Object[] params) {
        doLog(Level.ERROR, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of ERROR.
     *
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #errorv(Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    public void error(Object message, Object[] params, Throwable t) {
        doLog(Level.ERROR, FQCN, message, params, t);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of ERROR.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public void error(String loggerFqcn, Object message, Object[] params, Throwable t) {
        doLog(Level.ERROR, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public void errorv(String format, Object... params) {
        doLog(Level.ERROR, FQCN, format, params, null);
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void errorv(String format, Object param1) {
        if (isEnabled(Level.ERROR)) {
            doLog(Level.ERROR, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void errorv(String format, Object param1, Object param2) {
        if (isEnabled(Level.ERROR)) {
            doLog(Level.ERROR, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void errorv(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.ERROR)) {
            doLog(Level.ERROR, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public void errorv(Throwable t, String format, Object... params) {
        doLog(Level.ERROR, FQCN, format, params, t);
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void errorv(Throwable t, String format, Object param1) {
        if (isEnabled(Level.ERROR)) {
            doLog(Level.ERROR, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a log message with a level of ERROR using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void errorv(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.ERROR)) {
            doLog(Level.ERROR, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void errorv(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.ERROR)) {
            doLog(Level.ERROR, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public void errorf(String format, Object... params) {
        doLogf(Level.ERROR, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public void errorf(String format, Object param1) {
        if (isEnabled(Level.ERROR)) {
            doLogf(Level.ERROR, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void errorf(String format, Object param1, Object param2) {
        if (isEnabled(Level.ERROR)) {
            doLogf(Level.ERROR, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void errorf(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.ERROR)) {
            doLogf(Level.ERROR, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public void errorf(Throwable t, String format, Object... params) {
        doLogf(Level.ERROR, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public void errorf(Throwable t, String format, Object param1) {
        if (isEnabled(Level.ERROR)) {
            doLogf(Level.ERROR, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of ERROR.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void errorf(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.ERROR)) {
            doLogf(Level.ERROR, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void errorf(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.ERROR)) {
            doLogf(Level.ERROR, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a log message with a level of FATAL.
     *
     * @param message the message
     */
    public void fatal(Object message) {
        doLog(Level.FATAL, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable with a level of FATAL.
     *
     * @param message the message
     * @param t the throwable
     */
    public void fatal(Object message, Throwable t) {
        doLog(Level.FATAL, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable with a level of FATAL and a specific logger class name.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    public void fatal(String loggerFqcn, Object message, Throwable t) {
        doLog(Level.FATAL, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters with a level of FATAL.
     *
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #fatalv(String, Object...)} is recommended.
     */
    @Deprecated
    public void fatal(Object message, Object[] params) {
        doLog(Level.FATAL, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of FATAL.
     *
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #fatalv(Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    public void fatal(Object message, Object[] params, Throwable t) {
        doLog(Level.FATAL, FQCN, message, params, t);
    }

    /**
     * Issue a log message with parameters and a throwable with a level of FATAL.
     *
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     */
    public void fatal(String loggerFqcn, Object message, Object[] params, Throwable t) {
        doLog(Level.FATAL, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param params the parameters
     */
    public void fatalv(String format, Object... params) {
        doLog(Level.FATAL, FQCN, format, params, null);
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void fatalv(String format, Object param1) {
        if (isEnabled(Level.FATAL)) {
            doLog(Level.FATAL, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void fatalv(String format, Object param1, Object param2) {
        if (isEnabled(Level.FATAL)) {
            doLog(Level.FATAL, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void fatalv(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.FATAL)) {
            doLog(Level.FATAL, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    public void fatalv(Throwable t, String format, Object... params) {
        doLog(Level.FATAL, FQCN, format, params, t);
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    public void fatalv(Throwable t, String format, Object param1) {
        if (isEnabled(Level.FATAL)) {
            doLog(Level.FATAL, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a log message with a level of FATAL using {@link java.text.MessageFormat}-style formatting.
     *
     * @param t the throwable
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void fatalv(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.FATAL)) {
            doLog(Level.FATAL, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void fatalv(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.FATAL)) {
            doLog(Level.FATAL, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    public void fatalf(String format, Object... params) {
        doLogf(Level.FATAL, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    public void fatalf(String format, Object param1) {
        if (isEnabled(Level.FATAL)) {
            doLogf(Level.FATAL, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void fatalf(String format, Object param1, Object param2) {
        if (isEnabled(Level.FATAL)) {
            doLogf(Level.FATAL, FQCN, format, new Object[] { param1, param2 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     * @param param3 the third parameter
     */
    public void fatalf(String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.FATAL)) {
            doLogf(Level.FATAL, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    public void fatalf(Throwable t, String format, Object... params) {
        doLogf(Level.FATAL, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    public void fatalf(Throwable t, String format, Object param1) {
        if (isEnabled(Level.FATAL)) {
            doLogf(Level.FATAL, FQCN, format, new Object[] { param1 }, t);
        }
    }

    /**
     * Issue a formatted log message with a level of FATAL.
     *
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    public void fatalf(Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(Level.FATAL)) {
            doLogf(Level.FATAL, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    public void fatalf(Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(Level.FATAL)) {
            doLogf(Level.FATAL, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Log a message at the given level.
     *
     * @param level the level
     * @param message the message
     */
    @AlwaysInline("Fast level checks")
    public void log(Level level, Object message) {
        doLog(level, FQCN, message, null, null);
    }

    /**
     * Issue a log message and throwable at the given log level.
     *
     * @param level the level
     * @param message the message
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void log(Level level, Object message, Throwable t) {
        doLog(level, FQCN, message, null, t);
    }

    /**
     * Issue a log message and throwable at the given log level and a specific logger class name.
     *
     * @param level the level
     * @param loggerFqcn the logger class name
     * @param message the message
     * @param t the throwable
     */
    @AlwaysInline("Fast level checks")
    public void log(Level level, String loggerFqcn, Object message, Throwable t) {
        doLog(level, loggerFqcn, message, null, t);
    }

    /**
     * Issue a log message with parameters at the given log level.
     *
     * @param level the level
     * @param message the message
     * @param params the message parameters
     * @deprecated To log a message with parameters, using {@link #logv(Level, String, Object...)} is recommended.
     */
    @Deprecated
    @AlwaysInline("Fast level checks")
    public void log(Level level, Object message, Object[] params) {
        doLog(level, FQCN, message, params, null);
    }

    /**
     * Issue a log message with parameters and a throwable at the given log level.
     *
     * @param level the level
     * @param message the message
     * @param params the message parameters
     * @param t the throwable
     * @deprecated To log a message with parameters, using {@link #logv(Level, Throwable, String, Object...)} is recommended.
     */
    @Deprecated
    @AlwaysInline("Fast level checks")
    public void log(Level level, Object message, Object[] params, Throwable t) {
        doLog(level, FQCN, message, params, t);
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
    @AlwaysInline("Fast level checks")
    public void log(String loggerFqcn, Level level, Object message, Object[] params, Throwable t) {
        doLog(level, loggerFqcn, message, params, t);
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void logv(Level level, String format, Object... params) {
        doLog(level, FQCN, format, params, null);
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void logv(Level level, String format, Object param1) {
        if (isEnabled(level)) {
            doLog(level, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param format the message format string
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void logv(Level level, String format, Object param1, Object param2) {
        if (isEnabled(level)) {
            doLog(level, FQCN, format, new Object[] { param1, param2 }, null);
        }
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
    @AlwaysInline("Fast level checks")
    public void logv(Level level, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(level)) {
            doLog(level, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void logv(Level level, Throwable t, String format, Object... params) {
        doLog(level, FQCN, format, params, t);
    }

    /**
     * Issue a log message at the given log level using {@link java.text.MessageFormat}-style formatting.
     *
     * @param level the level
     * @param t the throwable
     * @param format the message format string
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void logv(Level level, Throwable t, String format, Object param1) {
        if (isEnabled(level)) {
            doLog(level, FQCN, format, new Object[] { param1 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logv(Level level, Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(level)) {
            doLog(level, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logv(Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(level)) {
            doLog(level, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logv(String loggerFqcn, Level level, Throwable t, String format, Object... params) {
        doLog(level, loggerFqcn, format, params, t);
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
    @AlwaysInline("Fast level checks")
    public void logv(String loggerFqcn, Level level, Throwable t, String format, Object param1) {
        if (isEnabled(level)) {
            doLog(level, loggerFqcn, format, new Object[] { param1 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logv(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(level)) {
            doLog(level, loggerFqcn, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logv(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(level)) {
            doLog(level, loggerFqcn, format, new Object[] { param1, param2, param3 }, t);
        }
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void logf(Level level, String format, Object... params) {
        doLogf(level, FQCN, format, params, null);
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void logf(Level level, String format, Object param1) {
        if (isEnabled(level)) {
            doLogf(level, FQCN, format, new Object[] { param1 }, null);
        }
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param format the format string as per {@link String#format(String, Object...)} or resource bundle key therefor
     * @param param1 the first parameter
     * @param param2 the second parameter
     */
    @AlwaysInline("Fast level checks")
    public void logf(Level level, String format, Object param1, Object param2) {
        if (isEnabled(level)) {
            doLogf(level, FQCN, format, new Object[] { param1, param2 }, null);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(Level level, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(level)) {
            doLogf(level, FQCN, format, new Object[] { param1, param2, param3 }, null);
        }
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param params the parameters
     */
    @AlwaysInline("Fast level checks")
    public void logf(Level level, Throwable t, String format, Object... params) {
        doLogf(level, FQCN, format, params, t);
    }

    /**
     * Issue a formatted log message at the given log level.
     *
     * @param level the level
     * @param t the throwable
     * @param format the format string, as per {@link String#format(String, Object...)}
     * @param param1 the sole parameter
     */
    @AlwaysInline("Fast level checks")
    public void logf(Level level, Throwable t, String format, Object param1) {
        if (isEnabled(level)) {
            doLogf(level, FQCN, format, new Object[] { param1 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(Level level, Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(level)) {
            doLogf(level, FQCN, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(level)) {
            doLogf(level, FQCN, format, new Object[] { param1, param2, param3 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(String loggerFqcn, Level level, Throwable t, String format, Object param1) {
        if (isEnabled(level)) {
            doLogf(level, loggerFqcn, format, new Object[] { param1 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2) {
        if (isEnabled(level)) {
            doLogf(level, loggerFqcn, format, new Object[] { param1, param2 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(String loggerFqcn, Level level, Throwable t, String format, Object param1, Object param2, Object param3) {
        if (isEnabled(level)) {
            doLogf(level, loggerFqcn, format, new Object[] { param1, param2, param3 }, t);
        }
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
    @AlwaysInline("Fast level checks")
    public void logf(String loggerFqcn, Level level, Throwable t, String format, Object... params) {
        doLogf(level, loggerFqcn, format, params, t);
    }

    /**
     * Read resolver; replaces deserialized instance with a canonical instance.
     *
     * @return the canonical logger instance
     */
    protected final Object writeReplace() {
        return new SerializedLogger(name);
    }

    /**
     * Get a Logger instance given the logger name.
     *
     * @param name the logger name
     *
     * @return the logger
     */
    public static Logger getLogger(String name) {
        return LoggerProviders.PROVIDER.getLogger(name);
    }

    /**
     * Get a Logger instance given the logger name with the given suffix.
     * <p/>
     * <p>This will include a logger separator between logger name and suffix.
     *
     * @param name the logger name
     * @param suffix a suffix to append to the logger name
     *
     * @return the logger
     */
    public static Logger getLogger(String name, String suffix) {
        return getLogger(name == null || name.length() == 0 ? suffix : name + "." + suffix);
    }

    /**
     * Get a Logger instance given the name of a class. This simply calls create(clazz.getName()).
     *
     * @param clazz the Class whose name will be used as the logger name
     *
     * @return the logger
     */
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    /**
     * Get a Logger instance given the name of a class with the given suffix.
     * <p/>
     * <p>This will include a logger separator between logger name and suffix
     *
     * @param clazz the Class whose name will be used as the logger name
     * @param suffix a suffix to append to the logger name
     *
     * @return the logger
     */
    public static Logger getLogger(Class<?> clazz, String suffix) {
        return getLogger(clazz.getName(), suffix);
    }

    /**
     * Get a typed logger which implements the given interface.  The current default locale will be used for the new logger.
     *
     * @param type the interface to implement
     * @param category the logger category
     * @param <T> the logger type
     * @return the typed logger
     */
    public static <T> T getMessageLogger(Class<T> type, String category) {
        return getMessageLogger(type, category, LoggingLocale.getLocale());
    }

    /**
     * Get a typed logger which implements the given interface.  The given locale will be used for the new logger.
     *
     * @param type the interface to implement
     * @param category the logger category
     * @param locale the locale for the new logger
     * @param <T> the logger type
     * @return the typed logger
     */
    public static <T> T getMessageLogger(final Class<T> type, final String category, final Locale locale) {
        return doPrivileged(new PrivilegedAction<T>() {
            public T run() {
                String language = locale.getLanguage();
                String country = locale.getCountry();
                String variant = locale.getVariant();

                Class<? extends T> loggerClass = null;
                final ClassLoader classLoader = type.getClassLoader();
                final String typeName = type.getName();
                if (variant != null && variant.length() > 0) try {
                    loggerClass = Class.forName(join(typeName, "$logger", language, country, variant), true, classLoader).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (loggerClass == null && country != null && country.length() > 0) try {
                    loggerClass = Class.forName(join(typeName, "$logger", language, country, null), true, classLoader).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (loggerClass == null && language != null && language.length() > 0) try {
                    loggerClass = Class.forName(join(typeName, "$logger", language, null, null), true, classLoader).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    // ignore
                }
                if (loggerClass == null) try {
                    loggerClass = Class.forName(join(typeName, "$logger", null, null, null), true, classLoader).asSubclass(type);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException("Invalid logger " + type + " (implementation not found in " + classLoader + ")");
                }
                final Constructor<? extends T> constructor;
                try {
                    constructor = loggerClass.getConstructor(Logger.class);
                } catch (NoSuchMethodException e) {
                    throw new IllegalArgumentException("Logger implementation " + loggerClass + " has no matching constructor");
                }
                try {
                    return constructor.newInstance(Logger.getLogger(category));
                } catch (InstantiationException e) {
                    throw new IllegalArgumentException("Logger implementation " + loggerClass + " could not be instantiated", e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("Logger implementation " + loggerClass + " could not be instantiated", e);
                } catch (InvocationTargetException e) {
                    throw new IllegalArgumentException("Logger implementation " + loggerClass + " could not be instantiated", e.getCause());
                }
            }
        });
    }

    private static String join(String interfaceName, String a, String b, String c, String d) {
        final StringBuilder build = new StringBuilder();
        build.append(interfaceName).append('_').append(a);
        if (b != null && b.length() > 0) {
            build.append('_');
            build.append(b);
        }
        if (c != null && c.length() > 0) {
            build.append('_');
            build.append(c);
        }
        if (d != null && d.length() > 0) {
            build.append('_');
            build.append(d);
        }
        return build.toString();
    }
}