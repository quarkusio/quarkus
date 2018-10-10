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

package org.jboss.logmanager.filters;

import java.text.MessageFormat;
import java.util.logging.LogRecord;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.logging.Filter;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.ExtLogRecord.FormatStyle;

/**
 * A filter which applies a text substitution on the message if the nested filter matches.
 */
public final class SubstituteFilter implements Filter {

    private final Pattern pattern;
    private final String replacement;
    private final boolean replaceAll;

    /**
     * Construct a new instance.
     *
     * @param pattern the pattern to match
     * @param replacement the string replacement
     * @param replaceAll {@code true} if all occurrances should be replaced; {@code false} if only the first occurrance
     */
    public SubstituteFilter(final Pattern pattern, final String replacement, final boolean replaceAll) {
        this.pattern = pattern;
        this.replacement = replacement;
        this.replaceAll = replaceAll;
    }

    /**
     * Construct a new instance.
     *
     * @param patternString the pattern to match
     * @param replacement the string replacement
     * @param replaceAll {@code true} if all occurrances should be replaced; {@code false} if only the first occurrance
     */
    public SubstituteFilter(final String patternString, final String replacement, final boolean replaceAll) {
        this(Pattern.compile(patternString), replacement, replaceAll);
    }

    /**
     * Apply the filter to the given log record.
     * <p/>
     * The {@link FormatStyle format style} will always be set to {@link FormatStyle#NO_FORMAT} as the formatted
     * message will be the one used in the replacement.
     *
     * @param record the log record to inspect and modify
     *
     * @return {@code true} always
     */
    @Override
    public boolean isLoggable(final LogRecord record) {
        final String currentMsg;
        if (record instanceof ExtLogRecord) {
            currentMsg = ((ExtLogRecord) record).getFormattedMessage();
        } else {
            currentMsg = MessageFormat.format(record.getMessage(), record.getParameters());
        }
        final Matcher matcher = pattern.matcher(String.valueOf(currentMsg));
        final String msg;
        if (replaceAll) {
            msg = matcher.replaceAll(replacement);
        } else {
            msg = matcher.replaceFirst(replacement);
        }
        if (record instanceof ExtLogRecord) {
            ((ExtLogRecord) record).setMessage(msg, FormatStyle.NO_FORMAT);
        } else {
            record.setMessage(msg);
        }
        return true;
    }
}
