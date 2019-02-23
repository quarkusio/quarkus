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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.NamedBuildItem;
import org.wildfly.common.Assert;

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
        if (item instanceof NamedBuildItem) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        produce(new ItemId(item.getClass(), null), item);
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        produce(new ItemId(type, null), item);
        return this;
    }

    /**
     * Provide an initial item.
     *
     * @param name the build item name (must not be {@code null})
     * @param item the item value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code type},
     *         or if the item does not allow multiplicity but this method is called more than one time
     */
    public <N, T extends NamedBuildItem<N>> BuildExecutionBuilder produce(N name, T item) {
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("item", item);
        produce(new ItemId(item.getClass(), name), item);
        return this;
    }

    /**
     * Provide an initial item.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @param item the item value
     * @return this builder
     * @throws IllegalArgumentException if this deployer chain was not declared to initially produce {@code type},
     *         or if {@code type} is {@code null}, or if the item does not allow multiplicity but this method is called
     *         more than one time
     */
    public <N, T extends NamedBuildItem<N>> BuildExecutionBuilder produce(Class<T> type, N name, T item) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("item", item);
        produce(new ItemId(type, name), item);
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
