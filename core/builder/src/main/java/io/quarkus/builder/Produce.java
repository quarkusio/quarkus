package io.quarkus.builder;

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
