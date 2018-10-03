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
import java.util.logging.LogRecord;

/**
 * An inverting filter.
 */
public final class InvertFilter implements Filter {
    private final Filter target;

    /**
     * Construct a new instance.
     *
     * @param target the target filter
     */
    public InvertFilter(final Filter target) {
        this.target = target;
    }

    /**
     * Determine whether a log record passes this filter.
     *
     * @param record the log record
     * @return {@code true} if the target filter returns {@code false}, {@code false} otherwise
     */
    public boolean isLoggable(final LogRecord record) {
        return ! target.isLoggable(record);
    }
}
