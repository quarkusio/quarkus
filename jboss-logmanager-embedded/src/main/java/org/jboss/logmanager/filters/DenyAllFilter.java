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
 * A deny-all filter.
 */
public final class DenyAllFilter implements Filter {
    private DenyAllFilter() {}

    private static final DenyAllFilter INSTANCE = new DenyAllFilter();

    /**
     * Always returns {@code false}.
     *
     * @param record ignored
     * @return {@code false}
     */
    public boolean isLoggable(final LogRecord record) {
        return false;
    }

    /**
     * Get the filter instance.
     *
     * @return the filter instance
     */
    public static DenyAllFilter getInstance() {
        return INSTANCE;
    }
}
