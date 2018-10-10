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
import org.jboss.logmanager.ExtFormatter;
import static java.lang.Math.max;

/**
 * A formatter which formats a record in a series of steps.
 */
public class MultistepFormatter extends ExtFormatter {
    private volatile FormatStep[] steps;
    private volatile int builderLength;
    private volatile boolean callerCalculationRequired = false;

    private static final FormatStep[] EMPTY_STEPS = new FormatStep[0];

    /**
     * Construct a new instance.
     *
     * @param steps the steps to execute to format the record
     */
    public MultistepFormatter(final FormatStep[] steps) {
        this.steps = steps.clone();
        calculateBuilderLength();
    }

    private void calculateBuilderLength() {
        boolean callerCalculatedRequired = false;
        int builderLength = 0;
        for (FormatStep step : steps) {
            builderLength += step.estimateLength();
            if (step.isCallerInformationRequired()) {
                callerCalculatedRequired = true;
            }
        }
        this.builderLength = max(32, builderLength);
        this.callerCalculationRequired = callerCalculatedRequired;
    }

    /**
     * Construct a new instance.
     */
    public MultistepFormatter() {
        steps = EMPTY_STEPS;
    }

    /**
     * Get a copy of the format steps.
     *
     * @return a copy of the format steps
     */
    public FormatStep[] getSteps() {
        return steps.clone();
    }

    /**
     * Assign new format steps.
     *
     * @param steps the new format steps
     */
    public void setSteps(final FormatStep[] steps) {
        this.steps = steps == null || steps.length == 0 ? EMPTY_STEPS : steps.clone();
        calculateBuilderLength();
    }

    /** {@inheritDoc} */
    public String format(final ExtLogRecord record) {
        final StringBuilder builder = new StringBuilder(builderLength);
        for (FormatStep step : steps) {
            step.render(this, builder, record);
        }
        return builder.toString();
    }

    @Override
    public boolean isCallerCalculationRequired() {
        return callerCalculationRequired;
    }
}
