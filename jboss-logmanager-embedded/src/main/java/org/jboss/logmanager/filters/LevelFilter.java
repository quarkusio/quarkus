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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import java.util.logging.Filter;
import java.util.logging.Level;
import java.util.logging.LogRecord;

/**
 * A filter which excludes messages of a certain level or levels
 */
public final class LevelFilter implements Filter {
    private final Set<Level> includedLevels;

    /**
     * Construct a new instance.
     *
     * @param includedLevel the level to include
     */
    public LevelFilter(final Level includedLevel) {
        includedLevels = Collections.singleton(includedLevel);
    }

    /**
     * Construct a new instance.
     *
     * @param includedLevels the levels to exclude
     */
    public LevelFilter(final Collection<Level> includedLevels) {
        this.includedLevels = new HashSet<Level>(includedLevels);
    }

    /**
     * Determine whether the message is loggable.
     *
     * @param record the log record
     * @return {@code true} if the level is in the inclusion list
     */
    public boolean isLoggable(final LogRecord record) {
        return includedLevels.contains(record.getLevel());
    }
}