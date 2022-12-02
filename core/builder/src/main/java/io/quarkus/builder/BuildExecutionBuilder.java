package io.quarkus.builder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.BuildItem;

/**
 * A builder for a deployer execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildExecutionBuilder {
    private final BuildChain buildChain;
    private final String buildTargetName;
    private final Map<ItemId, BuildItem> initialSingle;
    private final Map<ItemId, ArrayList<BuildItem>> initialMulti;

    BuildExecutionBuilder(final BuildChain buildChain, final String buildTargetName) {
        this.buildChain = buildChain;
        this.buildTargetName = buildTargetName;
        initialSingle = new HashMap<>(buildChain.getInitialSingleCount());
        initialMulti = new HashMap<>(buildChain.getInitialMultiCount());
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
            final List<BuildItem> list = initialMulti.computeIfAbsent(id, x -> new ArrayList<>());
            if (Comparable.class.isAssignableFrom(id.getType())) {
                int pos = Collections.binarySearch((List) list, value);
                if (pos < 0)
                    pos = -(pos + 1);
                list.add(pos, value);
            } else {
                list.add(value);
            }
        } else {
            if (initialSingle.putIfAbsent(id, value) != null) {
                throw Messages.msg.cannotMulti(id);
            }
        }
    }

    Map<ItemId, BuildItem> getInitialSingle() {
        return initialSingle;
    }

    Map<ItemId, ArrayList<BuildItem>> getInitialMulti() {
        return initialMulti;
    }

    BuildChain getChain() {
        return buildChain;
    }
}
