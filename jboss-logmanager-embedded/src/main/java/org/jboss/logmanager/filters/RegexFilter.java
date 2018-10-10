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

import java.util.logging.LogRecord;
import java.util.regex.Pattern;

import java.util.logging.Filter;

/**
 * A regular-expression-based filter.  Used to exclude log records which match or don't match the expression.  The
 * regular expression is checked against the raw (unformatted) message.
 */
public final class RegexFilter implements Filter {
    private final Pattern pattern;

    /**
     * Create a new instance.
     *
     * @param pattern the pattern to match
     */
    public RegexFilter(final Pattern pattern) {
        this.pattern = pattern;
    }

    /**
     * Create a new instance.
     *
     * @param patternString the pattern string to match
     */
    public RegexFilter(final String patternString) {
        this(Pattern.compile(patternString));
    }

    /**
     * Determine if this log record is loggable.
     *
     * @param record the log record
     * @return {@code true} if the log record is loggable
     */
    @Override
    public boolean isLoggable(final LogRecord record) {
        return record != null && pattern.matcher(String.valueOf(record.getMessage())).find();
    }
}
