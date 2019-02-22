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
final class Consume {
    private final BuildStepBuilder buildStepBuilder;
    private final ItemId itemId;
    private final Constraint constraint;
    private final ConsumeFlags flags;

    Consume(final BuildStepBuilder buildStepBuilder, final ItemId itemId, final Constraint constraint,
            final ConsumeFlags flags) {
        this.buildStepBuilder = buildStepBuilder;
        this.itemId = itemId;
        this.constraint = constraint;
        this.flags = flags;
    }

    BuildStepBuilder getBuildStepBuilder() {
        return buildStepBuilder;
    }

    ItemId getItemId() {
        return itemId;
    }

    ConsumeFlags getFlags() {
        return flags;
    }

    Consume combine(final Constraint constraint, final ConsumeFlags flags) {
        final Constraint outputConstraint = constraint == Constraint.REAL || this.constraint == Constraint.REAL
                ? Constraint.REAL
                : Constraint.ORDER_ONLY;
        final ConsumeFlags outputFlags = !flags.contains(ConsumeFlag.OPTIONAL) || !this.flags.contains(ConsumeFlag.OPTIONAL)
                ? flags.with(this.flags).without(ConsumeFlag.OPTIONAL)
                : flags.with(this.flags);
        return new Consume(buildStepBuilder, itemId, outputConstraint, outputFlags);
    }

    Constraint getConstraint() {
        return constraint;
    }
}
