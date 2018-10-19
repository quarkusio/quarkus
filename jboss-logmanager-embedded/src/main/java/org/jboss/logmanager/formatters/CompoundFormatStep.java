/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
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

import java.util.logging.Formatter;

import org.jboss.logmanager.ExtLogRecord;

/**
 * A compound format step.  Create via {@link FormatStep#createCompoundStep(FormatStep...)}.
 */
public class CompoundFormatStep implements FormatStep {
    private final FormatStep[] steps;

    protected CompoundFormatStep(final FormatStep[] clonedSteps) {
        this.steps = clonedSteps;
    }

    public void render(final Formatter formatter, final StringBuilder builder, final ExtLogRecord record) {
        for (FormatStep step : steps) {
            step.render(formatter, builder, record);
        }
    }

    public void render(final StringBuilder builder, final ExtLogRecord record) {
        render(null, builder, record);
    }

    public int estimateLength() {
        int est = 0;
        for (FormatStep step : steps) {
            est += step.estimateLength();
        }
        return est;
    }

    public boolean isCallerInformationRequired() {
        for (FormatStep step : steps) {
            if (step.isCallerInformationRequired()) {
                return true;
            }
        }
        return false;
    }

    public ItemType getItemType() {
        return ItemType.COMPOUND;
    }

    public int childStepCount() {
        return steps.length;
    }

    public FormatStep getChildStep(final int idx) {
        return steps[idx];
    }

    public FormatStep[] childSteps() {
        return steps.clone();
    }
}
