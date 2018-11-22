/*
 * Copyright 2018 Red Hat, Inc.
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
     * An array of no steps.
     */
    FormatStep[] NO_STEPS = new FormatStep[0];

    FormatStep NULL_STEP = new FormatStep() {
        public void render(final StringBuilder builder, final ExtLogRecord record) {
        }

        public int estimateLength() {
            return 0;
        }
    };

    static FormatStep createCompoundStep(final FormatStep... steps) {
        final FormatStep[] clonedSteps = steps.clone();
        if (clonedSteps.length == 0) {
            return NULL_STEP;
        } else if (clonedSteps.length == 1) {
            return clonedSteps[0];
        } else return new CompoundFormatStep(clonedSteps);
    }

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

    /**
     * Get child steps that compose this step.
     *
     * @return the child steps (not {@code null})
     */
    default FormatStep[] childSteps() {
        return NO_STEPS;
    }

    default int childStepCount() {
        return 0;
    }

    default FormatStep getChildStep(int idx) {
        throw new IndexOutOfBoundsException();
    }

    /**
     * Get the item type of this step.
     *
     * @return the item type
     */
    default ItemType getItemType() {
        return ItemType.GENERIC;
    }

    /**
     * An enumeration of the types of items that can be rendered.  Note that this enumeration may be expanded
     * in the future, so unknown values should be handled gracefully as if {@link #GENERIC} were used.
     */
    enum ItemType {
        /** An item of unknown kind. */
        GENERIC,

        /** A compound step. */
        COMPOUND,

        // == // == //

        /** A log level. */
        LEVEL,

        // == // == //

        SOURCE_CLASS_NAME,
        DATE,
        SOURCE_FILE_NAME,
        HOST_NAME,
        SOURCE_LINE_NUMBER,
        LINE_SEPARATOR,
        CATEGORY,
        MDC,
        /**
         * The log message without the exception trace.
         */
        MESSAGE,
        EXCEPTION_TRACE,
        SOURCE_METHOD_NAME,
        SOURCE_MODULE_NAME,
        SOURCE_MODULE_VERSION,
        NDC,
        PROCESS_ID,
        PROCESS_NAME,
        RELATIVE_TIME,
        RESOURCE_KEY,
        SYSTEM_PROPERTY,
        TEXT,
        THREAD_ID,
        THREAD_NAME,
    }
}
