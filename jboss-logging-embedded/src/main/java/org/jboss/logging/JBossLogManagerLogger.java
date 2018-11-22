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

import com.oracle.svm.core.annotate.AlwaysInline;
import org.jboss.logmanager.ExtLogRecord;

final class JBossLogManagerLogger extends Logger {

    private static final long serialVersionUID = 7429618317727584742L;

    private final org.jboss.logmanager.Logger logger;

    JBossLogManagerLogger(final String name, final org.jboss.logmanager.Logger logger) {
        super(name);
        this.logger = logger;
    }

    @AlwaysInline("Fast level checks")
    public boolean isEnabled(final Level level) {
        return logger.isLoggable(translate(level));
    }

    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
        java.util.logging.Level translatedLevel = translate(level);
        if (logger.isLoggable(translatedLevel)) {
          if (parameters == null) {
            logger.log(loggerClassName, translatedLevel, String.valueOf(message), thrown);
          } else {
            logger.log(loggerClassName, translatedLevel, String.valueOf(message), ExtLogRecord.FormatStyle.MESSAGE_FORMAT, parameters, thrown);
          }
        }
    }

    protected void doLogf(final Level level, final String loggerClassName, final String format, final Object[] parameters, final Throwable thrown) {
        if (parameters == null) {
            logger.log(loggerClassName, translate(level), format, thrown);
        } else {
            logger.log(loggerClassName, translate(level), format, ExtLogRecord.FormatStyle.PRINTF, parameters, thrown);
        }
    }

    @AlwaysInline("Fast level checks")
    private static java.util.logging.Level translate(final Level level) {
        if (level == Level.TRACE) {
            return org.jboss.logmanager.Level.TRACE;
        } else if (level == Level.DEBUG) {
            return org.jboss.logmanager.Level.DEBUG;
        }
        return infoOrHigher(level);
    }

    @AlwaysInline("Fast level checks")
    private static java.util.logging.Level infoOrHigher(final Level level) {
        if (level == Level.INFO) {
            return org.jboss.logmanager.Level.INFO;
        } else if (level == Level.WARN) {
            return org.jboss.logmanager.Level.WARN;
        } else if (level == Level.ERROR) {
            return org.jboss.logmanager.Level.ERROR;
        } else if (level == Level.FATAL) {
            return org.jboss.logmanager.Level.FATAL;
        }
        return org.jboss.logmanager.Level.ALL;
    }
}
