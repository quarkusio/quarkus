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
 * A filter which modifies the log record with a new level if the nested filter evaluates {@code true} for that
 * record.
 */
public final class LevelChangingFilter implements Filter {

    private final Level newLevel;

    /**
     * Construct a new instance.
     *
     * @param newLevel the level to change to
     */
    public LevelChangingFilter(final Level newLevel) {
        this.newLevel = newLevel;
    }

    /**
     * Apply the filter to this log record.
     *
     * @param record the record to inspect and possibly update
     * @return {@code true} always
     */
    public boolean isLoggable(final LogRecord record) {
        record.setLevel(newLevel);
        return true;
    }
}
