package io.quarkus.builder;

import java.util.HashMap;
import java.util.Map;

import io.quarkus.builder.item.BuildItem;
import io.quarkus.builder.item.MultiBuildItem;
import io.smallrye.common.constraint.Assert;

/**
 * A builder for a deployer execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildExecutionBuilder {
    private final BuildChain buildChain;
    private final String buildTargetName;
    private final Map<ItemId, BuildItem> initialSingle;
    private final MultiBuildItems multis;

    BuildExecutionBuilder(final BuildChain buildChain, final String buildTargetName) {
        this.buildChain = buildChain;
        this.buildTargetName = buildTargetName;
        initialSingle = new HashMap<>();
        multis = new MultiBuildItems(buildChain.getProducingOrdinals());
    }

    /**
     * Get the name of this build target. The resultant string is useful for diagnostic messages and does not have
     * any other significance.
     *
     * @return the name of this build target (not {@code null})
     */
    public String getBuildTargetName() {
        return buildTargetName;
    }

    /**
     * Provide an initial item.
     *
     * @param item the item value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code type},
     *         or if the item does not allow multiplicity but this method is called more than one time
     */
    public <T extends BuildItem> BuildExecutionBuilder produce(T item) {
        Assert.checkNotNullParam("item", item);
        produce(new ItemId(item.getClass()), item);
        return this;
    }

    /**
     * Provide an initial item.
     *
     * @param type the item type (must not be {@code null})
     * @param item the item value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code type},
     *         or if {@code type} is {@code null}, or if the item does not allow multiplicity but this method is called
     *         more than one time
     */
    public <T extends BuildItem> BuildExecutionBuilder produce(Class<T> type, T item) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("item", item);
        produce(new ItemId(type), item);
        return this;
    }

    /**
     * Run the build. The chain may run in one or many threads.
     *
     * @return the build result (not {@code null})
     * @throws BuildException if build failed
     */
    public BuildResult execute() throws BuildException {
        return new Execution(this, buildChain.getFinalIds()).run();
    }

    // -- //

    private void produce(final ItemId id, final BuildItem value) {
        if (!buildChain.hasInitial(id)) {
            throw Messages.msg.undeclaredItem(id);
        }
        if (id.isMulti()) {
            multis.putInitial(id, (MultiBuildItem) value);
        } else {
            if (initialSingle.putIfAbsent(id, value) != null) {
                throw Messages.msg.cannotMulti(id);
            }
        }
    }

    Map<ItemId, BuildItem> getInitialSingle() {
        return initialSingle;
    }

    MultiBuildItems getMultis() {
        return multis;
    }

    BuildChain getChain() {
        return buildChain;
    }
}
