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

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * Log only messages that fall within a level range.
 */
public final class LevelRangeFilter implements Filter {
    private final int min;
    private final int max;
    private final boolean minInclusive;
    private final boolean maxInclusive;

    /**
     * Create a new instance.
     *
     * @param min the minimum (least severe) level, inclusive
     * @param minInclusive {@code true} if the {@code min} value is inclusive, {@code false} if it is exclusive
     * @param max the maximum (most severe) level, inclusive
     * @param maxInclusive {@code true} if the {@code max} value is inclusive, {@code false} if it is exclusive
     */
    public LevelRangeFilter(final Level min, final boolean minInclusive, final Level max, final boolean maxInclusive) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
        this.min = min.intValue();
        this.max = max.intValue();
        if (this.max < this.min) {
            throw new IllegalArgumentException("Max level cannot be less than min level");
        }
    }

    /**
     * Determine if a record is loggable.
     *
     * @param record the log record
     * @return {@code true} if the record's level falls within the range specified for this instance
     */
    public boolean isLoggable(final LogRecord record) {
        final int iv = record.getLevel().intValue();
        return (minInclusive ? min <= iv : min < iv) && (maxInclusive ? iv <= max : iv < max);
    }
}
