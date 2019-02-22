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

package org.jboss.builder;

/**
 */
final class Produce {
    private final BuildStepBuilder stepBuilder;
    private final ItemId itemId;
    private final Constraint constraint;
    private final ProduceFlags flags;

    Produce(final BuildStepBuilder stepBuilder, final ItemId itemId, final Constraint constraint, final ProduceFlags flags) {
        this.stepBuilder = stepBuilder;
        this.itemId = itemId;
        this.constraint = constraint;
        this.flags = flags;
    }

    Produce combine(final Constraint constraint, final ProduceFlags flags) {
        final Constraint outputConstraint;
        final ProduceFlags outputFlags;
        if (constraint == Constraint.REAL || this.constraint == Constraint.REAL) {
            outputConstraint = Constraint.REAL;
        } else {
            outputConstraint = Constraint.ORDER_ONLY;
        }
        if (!flags.contains(ProduceFlag.WEAK) || !this.flags.contains(ProduceFlag.WEAK)) {
            outputFlags = flags.with(this.flags).without(ProduceFlag.WEAK);
        } else {
            outputFlags = flags.with(this.flags);
        }
        return new Produce(stepBuilder, itemId, outputConstraint, outputFlags);
    }

    BuildStepBuilder getStepBuilder() {
        return stepBuilder;
    }

    ItemId getItemId() {
        return itemId;
    }

    Constraint getConstraint() {
        return constraint;
    }

    ProduceFlags getFlags() {
        return flags;
    }
}
