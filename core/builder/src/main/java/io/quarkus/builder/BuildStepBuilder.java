package io.quarkus.builder;

import java.util.function.BooleanSupplier;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.EmptyBuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.qlue.StepBuilder;
import io.smallrye.common.constraint.Assert;

/**
 * A builder for build step instances within a chain. A build step can consume and produce items. It may also register
 * a destructor for items it produces, which will be run (in indeterminate order) at the end of processing.
 *
 * @deprecated For raw steps, prefer {@link StepBuilder} instead.
 */
@Deprecated
public final class BuildStepBuilder {
    private final BuildChainBuilder buildChainBuilder;
    private final StepBuilder stepBuilder;

    BuildStepBuilder(final BuildChainBuilder buildChainBuilder, final StepBuilder stepBuilder) {
        this.buildChainBuilder = buildChainBuilder;
        this.stepBuilder = stepBuilder;
    }

    /**
     * This unused method has been deprecated.
     */
    @Deprecated
    public BuildStepBuilder setBuildStep(final BuildStep ignored) {
        throw Assert.unsupported();
    }

    /**
     * This build step should complete before any build steps which consume the given item {@code type} are initiated.
     * If no such build steps exist, no ordering constraint is enacted.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder beforeConsume(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.beforeConsume(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.beforeConsume(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            stepBuilder.beforeConsume(LegacyEmptyItem.class, type.asSubclass(EmptyBuildItem.class));
        }
        return this;
    }

    /**
     * This build step should complete before any build steps which consume the given item {@code type} are initiated.
     * If no such build steps exist, no ordering constraint is enacted.
     *
     * @param type the item type (must not be {@code null})
     * @param flag the producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder beforeConsume(Class<? extends BuildItem> type, ProduceFlag flag) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("flag", flag);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.beforeConsume(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class), flag.getRealFlag());
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.beforeConsume(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class), flag.getRealFlag());
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            stepBuilder.beforeConsume(LegacyEmptyItem.class, type.asSubclass(EmptyBuildItem.class), flag.getRealFlag());
        }
        return this;
    }

    /**
     * This build step should be initiated after any build steps which produce the given item {@code type} are completed.
     * If no such build steps exist, no ordering constraint is enacted.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder afterProduce(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.afterProduce(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.afterProduce(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            stepBuilder.afterProduce(LegacyEmptyItem.class, type.asSubclass(EmptyBuildItem.class));
        }
        return this;
    }

    /**
     * Similarly to {@link #beforeConsume(Class)}, establish that this build step must come before the consumer(s) of the
     * given item {@code type}; however, only one {@code producer} may exist for the given item. In addition, the
     * build step may produce an actual value for this item, which will be shared to all consumers during deployment.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder produces(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot produce an empty build item");
        }
        return this;
    }

    /**
     * Similarly to {@link #beforeConsume(Class)}, establish that this build step must come before the consumer(s) of the
     * given item {@code type}; however, only one {@code producer} may exist for the given item. In addition, the
     * build step may produce an actual value for this item, which will be shared to all consumers during deployment.
     *
     * @param type the item type (must not be {@code null})
     * @param flag the producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder produces(Class<? extends BuildItem> type, ProduceFlag flag) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("flag", flag);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class), flag.getRealFlag());
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class), flag.getRealFlag());
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot produce an empty build item");
        }
        return this;
    }

    /**
     * Similarly to {@link #beforeConsume(Class)}, establish that this build step must come before the consumer(s) of the
     * given item {@code type}; however, only one {@code producer} may exist for the given item. In addition, the
     * build step may produce an actual value for this item, which will be shared to all consumers during deployment.
     *
     * @param type the item type (must not be {@code null})
     * @param flag1 the first producer flag to apply (must not be {@code null})
     * @param flag2 the second producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder produces(Class<? extends BuildItem> type, ProduceFlag flag1, ProduceFlag flag2) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("flag1", flag1);
        Assert.checkNotNullParam("flag2", flag2);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class), flag1.getRealFlag(),
                    flag2.getRealFlag());
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class), flag1.getRealFlag(),
                    flag2.getRealFlag());
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot produce an empty build item");
        }
        return this;
    }

    /**
     * Similarly to {@link #beforeConsume(Class)}, establish that this build step must come before the consumer(s) of the
     * given item {@code type}; however, only one {@code producer} may exist for the given item. In addition, the
     * build step may produce an actual value for this item, which will be shared to all consumers during deployment.
     *
     * @param type the item type (must not be {@code null})
     * @param flags the producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder produces(Class<? extends BuildItem> type, ProduceFlags flags) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("flags", flags);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class), flags.getRealFlags());
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.produces(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class), flags.getRealFlags());
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot produce an empty build item");
        }
        return this;
    }

    /**
     * This build step consumes the given produced item. The item must be produced somewhere in the chain. If
     * no such producer exists, the chain will not be constructed; instead, an error will be raised.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder consumes(Class<? extends BuildItem> type) {
        Assert.checkNotNullParam("type", type);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.consumes(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class));
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.consumes(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class));
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot consume an empty build item");
        }
        return this;
    }

    /**
     * This build step consumes the given produced item. The item must be produced somewhere in the chain. If
     * no such producer exists, the chain will not be constructed; instead, an error will be raised.
     *
     * @param type the item type (must not be {@code null})
     * @param flags a set of flags which modify the consume operation (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder consumes(Class<? extends BuildItem> type, ConsumeFlags flags) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("flags", flags);
        if (MultiBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.consumes(LegacyMultiItem.class, type.asSubclass(MultiBuildItem.class), flags.getRealFlags());
        } else if (SimpleBuildItem.class.isAssignableFrom(type)) {
            stepBuilder.consumes(LegacySimpleItem.class, type.asSubclass(SimpleBuildItem.class), flags.getRealFlags());
        } else {
            assert EmptyBuildItem.class.isAssignableFrom(type);
            throw new IllegalArgumentException("Cannot consume an empty build item");
        }
        return this;
    }

    /**
     * Build this step into the chain.
     *
     * @return the chain builder that this step was added to
     */
    public BuildChainBuilder build() {
        stepBuilder.build();
        return buildChainBuilder;
    }

    /**
     * Build this step into the chain if the supplier returns {@code true}.
     *
     * @param supp the {@code boolean} supplier (must not be {@code null})
     * @return the chain builder that this step was added to, or {@code null} if it was not added
     */
    public BuildChainBuilder buildIf(BooleanSupplier supp) {
        return supp.getAsBoolean() ? build() : null;
    }

    /**
     * Get the backing step builder.
     *
     * @return the backing step builder (not {@code null})
     */
    public StepBuilder getStepBuilder() {
        return stepBuilder;
    }

    // -- //

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BuildStep [");
        builder.append(stepBuilder);
        builder.append("]");
        return builder.toString();
    }
}
