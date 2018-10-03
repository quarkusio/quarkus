/*
 * JBoss, Home of Professional Open Source.
 *
 * Copyright 2017 Red Hat, Inc., and individual contributors
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

package org.jboss.logmanager.formatters;

import org.jboss.logmanager.ExtLogRecord;
import org.jboss.logmanager.ExtLogRecord.FormatStyle;
import org.jboss.logmanager.MapTestUtils;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
abstract class AbstractTest extends MapTestUtils {

    ExtLogRecord createLogRecord(final String msg) {
        return createLogRecord(org.jboss.logmanager.Level.INFO, msg);
    }

    ExtLogRecord createLogRecord(final String format, final Object... args) {
        return createLogRecord(org.jboss.logmanager.Level.INFO, format, args);
    }

    private ExtLogRecord createLogRecord(final org.jboss.logmanager.Level level, final String msg) {
        return new ExtLogRecord(level, msg, getClass().getName());
    }

    ExtLogRecord createLogRecord(final org.jboss.logmanager.Level level, final String format, final Object... args) {
        final ExtLogRecord record = new ExtLogRecord(level, format, FormatStyle.PRINTF, getClass().getName());
        record.setParameters(args);
        return record;
    }

}
