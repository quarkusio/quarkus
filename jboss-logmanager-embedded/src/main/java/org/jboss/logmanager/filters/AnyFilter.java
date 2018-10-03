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

import java.util.Iterator;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * A filter consisting of several filters in a chain.  If any filter finds the log message to be loggable,
 * the message will be logged and subsequent filters will not be checked.  If there are no nested filters, this
 * instance always returns {@code false}.
 */
public final class AnyFilter implements Filter {
    private final Filter[] filters;

    /**
     * Construct a new instance.
     *
     * @param filters the constituent filters
     */
    public AnyFilter(final Filter[] filters) {
        this.filters = filters.clone();
    }

    /**
     * Construct a new instance.
     *
     * @param filters the constituent filters
     */
    public AnyFilter(final Iterable<Filter> filters) {
        this(filters.iterator());
    }

    /**
     * Construct a new instance.
     *
     * @param filters the constituent filters
     */
    public AnyFilter(final Iterator<Filter> filters) {
        this.filters = unroll(filters, 0);
    }

    private static Filter[] unroll(Iterator<Filter> iter, int cnt) {
        if (iter.hasNext()) {
            final Filter filter = iter.next();
            if (filter == null) {
                throw new NullPointerException("filter at index " + cnt + " is null");
            }
            final Filter[] filters = unroll(iter, cnt + 1);
            filters[cnt] = filter;
            return filters;
        } else {
            return new Filter[cnt];
        }
    }

    /**
     * Determine whether the record is loggable.
     *
     * @param record the log record
     * @return {@code true} if any of the constituent filters return {@code true}
     */
    public boolean isLoggable(final LogRecord record) {
        for (Filter filter : filters) {
            if (filter.isLoggable(record)) {
                return true;
            }
        }
        return false;
    }
}