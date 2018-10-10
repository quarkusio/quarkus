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

package org.jboss.logmanager.formatters;

import org.jboss.logmanager.ExtLogRecord;

import java.util.logging.Formatter;

/**
 * A single format step which handles some part of rendering a log record.
 */
public interface FormatStep {

    /**
     * Render a part of the log record.
     *
     * @param builder the string builder to append to
     * @param record the record being rendered
     */
    void render(StringBuilder builder, ExtLogRecord record);

    /**
     * Render a part of the log record to the given formatter.
     *
     * @param formatter the formatter to render to
     * @param builder the string builder to append to
     * @param record the record being rendered
     */
    default void render(Formatter formatter, StringBuilder builder, ExtLogRecord record) {
        render(builder, record);
    }

    /**
     * Emit an estimate of the length of data which this step will produce.  The more accurate the estimate, the
     * more likely the format operation will be performant.
     *
     * @return an estimate
     */
    int estimateLength();

    /**
     * Indicates whether or not caller information is required for this format step.
     *
     * @return {@code true} if caller information is required, otherwise {@code false}
     */
    default boolean isCallerInformationRequired() {
        return false;
    }
}
