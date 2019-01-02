/*
 * Copyright 2018 Red Hat, Inc.
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

import java.text.MessageFormat;

final class Slf4jLogger extends Logger {

    private static final long serialVersionUID = 8685757928087758380L;

    private final org.slf4j.Logger logger;

    Slf4jLogger(final String name, final org.slf4j.Logger logger) {
        super(name);
        this.logger = logger;
    }

    public boolean isEnabled(final Level level) {
        if (level == Level.TRACE) {
            return logger.isTraceEnabled();
        } else if (level == Level.DEBUG) {
            return logger.isDebugEnabled();
        }
        return infoOrHigherEnabled(level);
    }

    private boolean infoOrHigherEnabled(final Level level) {
        if (level == Level.INFO) {
            return logger.isInfoEnabled();
        } else if (level == Level.WARN) {
            return logger.isWarnEnabled();
        } else if (level == Level.ERROR || level == Level.FATAL) {
            return logger.isErrorEnabled();
        }
        return true;
    }

    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
        if (isEnabled(level)) try {
            final String text = parameters == null || parameters.length == 0 ? String.valueOf(message) : MessageFormat.format(String.valueOf(message), parameters);
            if (level == Level.INFO) {
                logger.info(text, thrown);
            } else if (level == Level.WARN) {
                logger.warn(text, thrown);
            } else if (level == Level.ERROR || level == Level.FATAL) {
                logger.error(text, thrown);
            } else if (level == Level.DEBUG) {
                logger.debug(text, thrown);
            } else if (level == Level.TRACE) {
                logger.debug(text, thrown);
            }
        } catch (Throwable ignored) {}
    }

    protected void doLogf(final Level level, final String loggerClassName, final String format, final Object[] parameters, final Throwable thrown) {
        if (isEnabled(level)) try {
            final String text = parameters == null ? String.format(format) : String.format(format, parameters);
            if (level == Level.INFO) {
                logger.info(text, thrown);
            } else if (level == Level.WARN) {
                logger.warn(text, thrown);
            } else if (level == Level.ERROR || level == Level.FATAL) {
                logger.error(text, thrown);
            } else if (level == Level.DEBUG) {
                logger.debug(text, thrown);
            } else if (level == Level.TRACE) {
                logger.debug(text, thrown);
            }
        } catch (Throwable ignored) {}
    }
}
