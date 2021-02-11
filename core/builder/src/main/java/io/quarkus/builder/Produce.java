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
        ProduceFlags outputFlags;
        if (constraint == Constraint.REAL || this.constraint == Constraint.REAL) {
            outputConstraint = Constraint.REAL;
        } else {
            outputConstraint = Constraint.ORDER_ONLY;
        }
        outputFlags = flags.with(this.flags);
        if (!flags.contains(ProduceFlag.WEAK) || !this.flags.contains(ProduceFlag.WEAK)) {
            outputFlags = outputFlags.without(ProduceFlag.WEAK);
        }
        if (!flags.contains(ProduceFlag.OVERRIDABLE) || !this.flags.contains(ProduceFlag.OVERRIDABLE)) {
            outputFlags = outputFlags.without(ProduceFlag.OVERRIDABLE);
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

    boolean isOverridable() {
        return flags.contains(ProduceFlag.OVERRIDABLE);
    }
}
