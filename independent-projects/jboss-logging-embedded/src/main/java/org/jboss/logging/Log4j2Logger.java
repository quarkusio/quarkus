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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggingException;
import org.apache.logging.log4j.message.MessageFormatMessageFactory;
import org.apache.logging.log4j.message.StringFormattedMessage;
import org.apache.logging.log4j.spi.AbstractLogger;

final class Log4j2Logger extends Logger {

    private static final long serialVersionUID = -2507841068232627725L;

    private final AbstractLogger logger;
    private final MessageFormatMessageFactory messageFactory;

    Log4j2Logger(final String name) {
        super(name);
        org.apache.logging.log4j.Logger logger = LogManager.getLogger(name);
        if (!(logger instanceof AbstractLogger)) {
            throw new LoggingException("The logger for [" + name + "] does not extend AbstractLogger. Actual logger: " + logger.getClass().getName());
        }
        this.logger = (AbstractLogger)logger;
        this.messageFactory = new MessageFormatMessageFactory();
    }

    @Override
    public boolean isEnabled(final Level level) {
        return this.logger.isEnabled(Log4j2Logger.translate(level));
    }

    @Override
    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
        final org.apache.logging.log4j.Level translatedLevel = Log4j2Logger.translate(level);
        if (this.logger.isEnabled(translatedLevel)) {
            try {
                this.logger.logMessage(loggerClassName, translatedLevel, null,
                        (parameters == null || parameters.length == 0) ? this.messageFactory.newMessage(message) : this.messageFactory.newMessage(String.valueOf(message), parameters),
                        thrown);
            } catch (Throwable ignored) { }
        }
    }

    @Override
    protected void doLogf(final Level level, final String loggerClassName, final String format, final Object[] parameters, final Throwable thrown) {
        final org.apache.logging.log4j.Level translatedLevel = Log4j2Logger.translate(level);
        if (this.logger.isEnabled(translatedLevel)) {
            try {
                this.logger.logMessage(loggerClassName, translatedLevel, null, new StringFormattedMessage(format, parameters), thrown);
            } catch (Throwable ignored) { }
        }
    }

    private static org.apache.logging.log4j.Level translate(final Level level) {
        if (level == Level.TRACE) {
            return org.apache.logging.log4j.Level.TRACE;
        } else if (level == Level.DEBUG) {
            return org.apache.logging.log4j.Level.DEBUG;
        }
        return infoOrHigher(level);
    }

    private static org.apache.logging.log4j.Level infoOrHigher(final Level level) {
        if (level == Level.INFO) {
            return org.apache.logging.log4j.Level.INFO;
        } else if (level == Level.WARN) {
            return org.apache.logging.log4j.Level.WARN;
        } else if (level == Level.ERROR) {
            return org.apache.logging.log4j.Level.ERROR;
        } else if (level == Level.FATAL) {
            return org.apache.logging.log4j.Level.FATAL;
        }
        return org.apache.logging.log4j.Level.ALL;
    }
}
