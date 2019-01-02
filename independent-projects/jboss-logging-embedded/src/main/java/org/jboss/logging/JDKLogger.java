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

import java.util.MissingResourceException;
import java.util.ResourceBundle;

final class JDKLogger extends Logger {

    private static final long serialVersionUID = 2563174097983721393L;

    @SuppressWarnings({ "NonConstantLogger" })
    private transient final java.util.logging.Logger logger;

    public JDKLogger(final String name) {
        super(name);
        logger = java.util.logging.Logger.getLogger(name);
    }

    protected void doLog(final Level level, final String loggerClassName, final Object message, final Object[] parameters, final Throwable thrown) {
        if (isEnabled(level)) try {
            final JBossLogRecord rec = new JBossLogRecord(translate(level), String.valueOf(message), loggerClassName);
            if (thrown != null) rec.setThrown(thrown);
            rec.setLoggerName(getName());
            rec.setParameters(parameters);
            rec.setResourceBundleName(logger.getResourceBundleName());
            rec.setResourceBundle(logger.getResourceBundle());
            logger.log(rec);
        } catch (Throwable ignored) {}
    }

    protected void doLogf(final Level level, final String loggerClassName, String format, final Object[] parameters, final Throwable thrown) {
        if (isEnabled(level)) try {
            final ResourceBundle resourceBundle = logger.getResourceBundle();
            if (resourceBundle != null) try {
                format = resourceBundle.getString(format);
            } catch (MissingResourceException e) {
                // ignore
            }
            final String msg = parameters == null ? String.format(format) : String.format(format, parameters);
            final JBossLogRecord rec = new JBossLogRecord(translate(level), msg, loggerClassName);
            if (thrown != null) rec.setThrown(thrown);
            rec.setLoggerName(getName());
            rec.setResourceBundleName(logger.getResourceBundleName());
            // we've done all the business
            rec.setResourceBundle(null);
            rec.setParameters(null);
            logger.log(rec);
        } catch (Throwable ignored) {}
    }

    private static java.util.logging.Level translate(final Level level) {
        if (level == Level.TRACE) {
            return JDKLevel.TRACE;
        } else if (level == Level.DEBUG) {
            return JDKLevel.DEBUG;
        }
        return infoOrHigher(level);
    }

    private static java.util.logging.Level infoOrHigher(final Level level) {
        if (level == Level.INFO) {
            return JDKLevel.INFO;
        } else if (level == Level.WARN) {
            return JDKLevel.WARN;
        } else if (level == Level.ERROR) {
            return JDKLevel.ERROR;
        } else if (level == Level.FATAL) {
            return JDKLevel.FATAL;
        }
        return JDKLevel.ALL;
    }

    public boolean isEnabled(final Level level) {
        return logger.isLoggable(translate(level));
    }
}
