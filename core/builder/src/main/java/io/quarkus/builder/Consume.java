package io.quarkus.builder;

import static io.quarkus.builder.Constraint.ORDER_ONLY;
import static io.quarkus.builder.Constraint.REAL;
import static io.quarkus.builder.ConsumeFlag.OPTIONAL;

record Consume(BuildStepBuilder buildStepBuilder, ItemId itemId, Constraint constraint, ConsumeFlags flags) {

    Consume combine(final Constraint constraint, final ConsumeFlags flags) {
        return new Consume(
                buildStepBuilder,
                itemId,
                constraint == REAL || this.constraint == REAL
                        ? REAL
                        : ORDER_ONLY,
                !flags.contains(OPTIONAL) || !this.flags.contains(OPTIONAL)
                        ? flags.with(this.flags).without(OPTIONAL)
                        : flags.with(this.flags));
    }
}
