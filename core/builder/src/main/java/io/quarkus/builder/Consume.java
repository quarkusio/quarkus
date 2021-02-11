package io.quarkus.builder;

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
