/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2010 Red Hat, Inc.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.text.MessageFormat;
import org.slf4j.spi.LocationAwareLogger;

final class Slf4jLocationAwareLogger extends Logger {

    private static final long serialVersionUID = 8685757928087758380L;

    private static final Object[] EMPTY = new Object[0];
    private static final boolean POST_1_6;
    private static final Method LOG_METHOD;

    static {
        Method[] methods = LocationAwareLogger.class.getDeclaredMethods();
        Method logMethod = null;
        boolean post16 = false;
        for (Method method : methods) {
            if (method.getName().equals("log")) {
                logMethod = method;
                Class<?>[] parameterTypes = method.getParameterTypes();
                post16 = parameterTypes.length == 6;
            }
        }
        if (logMethod == null) {
            throw new NoSuchMethodError("Cannot find LocationAwareLogger.log() method");
        }
        POST_1_6 = post16;
        LOG_METHOD = logMethod;
    }

    private final LocationAwareLogger logger;

    Slf4jLocationAwareLogger(final String name, final LocationAwareLogger logger) {
        super(name);
        this.logger = logger;
    }

    public boolean isEnabled(final Level level) {
        if (level != null) switch (level) {
            case FATAL: return logger.isErrorEnabled();
            case ERROR: return logger.isErrorEnabled();
            case WARN:  return logger.isWarnEnabled();
            case INFO:  return logger.isInfoEnabled();
            case DEBUG: return logger.isDebugEnabled();
            case TRACE: return logger.isTraceEnabled();
        }
        return true;
    }

    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
        if (isEnabled(level)) {
            final String text = parameters == null || parameters.length == 0 ? String.valueOf(message) : MessageFormat.format(String.valueOf(message), parameters);
            doLog(logger, loggerClassName, translate(level), text, thrown);
        }
    }

    protected void doLogf(final Level level, final String loggerClassName, final String format, final Object[] parameters, final Throwable thrown) {
        if (isEnabled(level)) {
            final String text = parameters == null ? String.format(format) : String.format(format, parameters);
            doLog(logger, loggerClassName, translate(level), text, thrown);
        }
    }

    private static void doLog(LocationAwareLogger logger, String className, int level, String text, Throwable thrown) {
        try {
            if (POST_1_6) {
                LOG_METHOD.invoke(logger, null, className, Integer.valueOf(level), text, EMPTY, thrown);
            } else {
                LOG_METHOD.invoke(logger, null, className, Integer.valueOf(level), text, thrown);
            }
        } catch (InvocationTargetException e) {
            try {
                throw e.getCause();
            } catch (RuntimeException ex) {
                throw ex;
            } catch (Error er) {
                throw er;
            } catch (Throwable throwable) {
                throw new UndeclaredThrowableException(throwable);
            }
        } catch (IllegalAccessException e) {
            throw new IllegalAccessError(e.getMessage());
        }
    }

    private static int translate(Level level) {
        if (level != null) switch (level) {
            case FATAL:
            case ERROR: return LocationAwareLogger.ERROR_INT;
            case WARN:  return LocationAwareLogger.WARN_INT;
            case INFO:  return LocationAwareLogger.INFO_INT;
            case DEBUG: return LocationAwareLogger.DEBUG_INT;
            case TRACE: return LocationAwareLogger.TRACE_INT;
        }
        return LocationAwareLogger.TRACE_INT;
    }
}