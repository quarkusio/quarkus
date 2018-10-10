/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2014 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager;

import java.text.MessageFormat;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * A formatter which handles {@link org.jboss.logmanager.ExtLogRecord ExtLogRecord} instances.
 */
public abstract class ExtFormatter extends Formatter {

    /** {@inheritDoc} */
    public final String format(final LogRecord record) {
        return format(ExtLogRecord.wrap(record));
    }

    /**
     * Format a message using an extended log record.
     *
     * @param record the log record
     * @return the formatted message
     */
    public abstract String format(ExtLogRecord record);

    @Override
    public String formatMessage(LogRecord record) {
        final ResourceBundle bundle = record.getResourceBundle();
        String msg = record.getMessage();
        if (msg == null) {
            return null;
        }
        if (bundle != null) {
            try {
                msg = bundle.getString(msg);
            } catch (MissingResourceException ex) {
                // ignore
            }
        }
        final Object[] parameters = record.getParameters();
        if (parameters == null || parameters.length == 0) {
            return formatMessageNone(record);
        }
        if (record instanceof ExtLogRecord) {
            final ExtLogRecord extLogRecord = (ExtLogRecord) record;
            final ExtLogRecord.FormatStyle formatStyle = extLogRecord.getFormatStyle();
            if (formatStyle == ExtLogRecord.FormatStyle.PRINTF) {
                return formatMessagePrintf(record);
            } else if (formatStyle == ExtLogRecord.FormatStyle.NO_FORMAT) {
                return formatMessageNone(record);
            }
        }
        return msg.indexOf('{') >= 0 ? formatMessageLegacy(record) : formatMessageNone(record);
    }

    /**
     * Determines whether or not this formatter will require caller, source level, information when a log record is
     * formatted.
     *
     * @return {@code true} if the formatter will need caller information, otherwise {@code false}
     *
     * @see LogRecord#getSourceClassName()
     * @see ExtLogRecord#getSourceFileName()
     * @see ExtLogRecord#getSourceLineNumber()
     * @see LogRecord#getSourceMethodName()
     */
    public boolean isCallerCalculationRequired() {
        return true;
    }

    /**
     * Format the message text as if there are no parameters.  The default implementation delegates to
     * {@link LogRecord#getMessage() record.getMessage()}.
     *
     * @param record the record to format
     * @return the formatted string
     */
    protected String formatMessageNone(LogRecord record) {
        return record.getMessage();
    }

    /**
     * Format the message text as if there are no parameters.  The default implementation delegates to
     * {@link MessageFormat#format(String, Object[]) MessageFormat.format(record.getMessage(),record.getParameters())}.
     *
     * @param record the record to format
     * @return the formatted string
     */
    protected String formatMessageLegacy(LogRecord record) {
        return MessageFormat.format(record.getMessage(), record.getParameters());
    }

    /**
     * Format the message text as if there are no parameters.  The default implementation delegates to
     * {@link String#format(String, Object[]) String.format(record.getMessage(),record.getParameters())}.
     *
     * @param record the record to format
     * @return the formatted string
     */
    protected String formatMessagePrintf(LogRecord record) {
        return String.format(record.getMessage(), record.getParameters());
    }
}