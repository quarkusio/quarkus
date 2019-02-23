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

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.NamedBuildItem;
import org.jboss.builder.item.SymbolicBuildItem;
import org.wildfly.common.Assert;

/**
 * A builder for build step instances within a chain. A build step can consume and produce items. It may also register
 * a destructor for items it produces, which will be run (in indeterminate order) at the end of processing.
 */
public final class BuildStepBuilder {
    private final BuildChainBuilder buildChainBuilder;
    private final Map<ItemId, Consume> consumes = new HashMap<>();
    private final Map<ItemId, Produce> produces = new HashMap<>();
    private BuildStep buildStep;

    BuildStepBuilder(final BuildChainBuilder buildChainBuilder) {
        this.buildChainBuilder = buildChainBuilder;
    }

    /**
     * Set the build step for this builder. If no build step is specified, then this step will be excluded from
     * the final chain.
     *
     * @param buildStep the build step
     * @return this builder
     */
    public BuildStepBuilder setBuildStep(final BuildStep buildStep) {
        this.buildStep = buildStep;
        return this;
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot consume a named build item without a name");
        }
        addProduces(new ItemId(type, null), Constraint.ORDER_ONLY, ProduceFlags.NONE);
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot consume a named build item without a name");
        }
        addProduces(new ItemId(type, null), Constraint.ORDER_ONLY, ProduceFlags.of(flag));
        return this;
    }

    /**
     * This build step should complete before any build steps which consume the given item {@code type} are initiated.
     * If no such build steps exist, no ordering constraint is enacted.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @return this builder
     */
    public <N> BuildStepBuilder beforeConsume(Class<? extends NamedBuildItem<N>> type, N name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        addProduces(new ItemId(type, name), Constraint.ORDER_ONLY, ProduceFlags.NONE);
        return this;
    }

    /**
     * This build step should complete before any build steps which consume the given item {@code type} are initiated.
     * If no such build steps exist, no ordering constraint is enacted.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @param flag the producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public <N> BuildStepBuilder beforeConsume(Class<? extends NamedBuildItem<N>> type, N name, ProduceFlag flag) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("flag", flag);
        addProduces(new ItemId(type, name), Constraint.ORDER_ONLY, ProduceFlags.of(flag));
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        addConsumes(new ItemId(type, null), Constraint.ORDER_ONLY, ConsumeFlags.of(ConsumeFlag.OPTIONAL));
        return this;
    }

    /**
     * This build step should be initiated after any build steps which produce the given item {@code type} are completed.
     * If no such build steps exist, no ordering constraint is enacted.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @return this builder
     */
    public <N> BuildStepBuilder afterProduce(Class<? extends NamedBuildItem<N>> type, N name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        addConsumes(new ItemId(type, name), Constraint.ORDER_ONLY, ConsumeFlags.of(ConsumeFlag.OPTIONAL));
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        addProduces(new ItemId(type, null), Constraint.REAL, ProduceFlags.NONE);
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        addProduces(new ItemId(type, null), Constraint.REAL, ProduceFlags.of(flag));
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
        Assert.checkNotNullParam("flag", flags);
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot produce a named build item without a name");
        }
        addProduces(new ItemId(type, null), Constraint.REAL, flags);
        return this;
    }

    /**
     * Similarly to {@link #beforeConsume(Class)}, establish that this build step must come before the consumer(s) of the
     * given item {@code type}; however, only one {@code producer} may exist for the given item. In addition, the
     * build step may produce an actual value for this item, which will be shared to all consumers during deployment.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @return this builder
     */
    public <N> BuildStepBuilder produces(Class<? extends NamedBuildItem<N>> type, N name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        addProduces(new ItemId(type, name), Constraint.REAL, ProduceFlags.NONE);
        return this;
    }

    /**
     * Similarly to {@link #beforeConsume(Class)}, establish that this build step must come before the consumer(s) of the
     * given item {@code type}; however, only one {@code producer} may exist for the given item. In addition, the
     * build step may produce an actual value for this item, which will be shared to all consumers during deployment.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @param flag the producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public <N> BuildStepBuilder produces(Class<? extends NamedBuildItem<N>> type, N name, ProduceFlag flag) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        Assert.checkNotNullParam("flag", flag);
        addProduces(new ItemId(type, name), Constraint.REAL, ProduceFlags.of(flag));
        return this;
    }

    /**
     * Declare that the build step "produces" a virtual item with the given identifier.
     *
     * @param symbolic the item identifier (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder beforeVirtual(Enum<?> symbolic) {
        Assert.checkNotNullParam("symbolic", symbolic);
        addProduces(new ItemId(SymbolicBuildItem.class, symbolic), Constraint.ORDER_ONLY, ProduceFlags.NONE);
        return this;
    }

    /**
     * Declare that the build step "produces" a virtual item with the given identifier.
     *
     * @param symbolic the item identifier (must not be {@code null})
     * @param flag the producer flag to apply (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder beforeVirtual(Enum<?> symbolic, ProduceFlag flag) {
        Assert.checkNotNullParam("symbolic", symbolic);
        Assert.checkNotNullParam("flag", flag);
        addProduces(new ItemId(SymbolicBuildItem.class, symbolic), Constraint.ORDER_ONLY, ProduceFlags.of(flag));
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot consume a named build item without a name");
        }
        addConsumes(new ItemId(type, null), Constraint.REAL, ConsumeFlags.NONE);
        return this;
    }

    /**
     * This build step consumes the given produced item. The item must be produced somewhere in the chain. If
     * no such producer exists, the chain will not be constructed; instead, an error will be raised.
     *
     * @param type the item type (must not be {@code null})
     * @param name the build item name (must not be {@code null})
     * @return this builder
     */
    public <N> BuildStepBuilder consumes(Class<? extends NamedBuildItem<N>> type, N name) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        addConsumes(new ItemId(type, name), Constraint.REAL, ConsumeFlags.NONE);
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
        if (NamedBuildItem.class.isAssignableFrom(type)) {
            throw new IllegalArgumentException("Cannot consume a named build item without a name");
        }
        addConsumes(new ItemId(type, null), Constraint.REAL, flags);
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
    public <N> BuildStepBuilder consumes(Class<? extends NamedBuildItem<N>> type, N name, ConsumeFlags flags) {
        Assert.checkNotNullParam("type", type);
        Assert.checkNotNullParam("name", name);
        addConsumes(new ItemId(type, name), Constraint.REAL, flags);
        return this;
    }

    /**
     * Declare that the build step "consumes" a virtual item with the given identifier.
     *
     * @param symbolic the item identifier (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder afterVirtual(Enum<?> symbolic) {
        addConsumes(new ItemId(SymbolicBuildItem.class, symbolic), Constraint.REAL, ConsumeFlags.NONE);
        return this;
    }

    /**
     * Declare that the build step "consumes" a virtual item with the given identifier.
     *
     * @param symbolic the item identifier (must not be {@code null})
     * @param flags a set of flags which modify the consume operation (must not be {@code null})
     * @return this builder
     */
    public BuildStepBuilder afterVirtual(Enum<?> symbolic, ConsumeFlags flags) {
        addConsumes(new ItemId(SymbolicBuildItem.class, symbolic), Constraint.REAL, flags);
        return this;
    }

    /**
     * Build this step into the chain.
     *
     * @return the chain builder that this step was added to
     */
    public BuildChainBuilder build() {
        final BuildChainBuilder chainBuilder = this.buildChainBuilder;
        chainBuilder.addStep(this, new Exception().getStackTrace());
        return chainBuilder;
    }

    // -- //

    BuildStep getBuildStep() {
        return buildStep;
    }

    private void addConsumes(final ItemId itemId, final Constraint constraint, final ConsumeFlags flags) {
        Assert.checkNotNullParam("flags", flags);
        consumes.compute(itemId,
                (id, c) -> c == null ? new Consume(this, itemId, constraint, flags) : c.combine(constraint, flags));
    }

    private void addProduces(final ItemId itemId, final Constraint constraint, final ProduceFlags flags) {
        produces.compute(itemId,
                (id, p) -> p == null ? new Produce(this, itemId, constraint, flags) : p.combine(constraint, flags));
    }

    Map<ItemId, Consume> getConsumes() {
        return consumes;
    }

    Map<ItemId, Produce> getProduces() {
        return produces;
    }

    Set<ItemId> getRealConsumes() {
        final HashMap<ItemId, Consume> map = new HashMap<>(consumes);
        map.entrySet().removeIf(e -> e.getValue().getConstraint() == Constraint.ORDER_ONLY);
        return map.keySet();
    }

    Set<ItemId> getRealProduces() {
        final HashMap<ItemId, Produce> map = new HashMap<>(produces);
        map.entrySet().removeIf(e -> e.getValue().getConstraint() == Constraint.ORDER_ONLY);
        return map.keySet();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("BuildStep [");
        builder.append(buildStep);
        builder.append("]");
        return builder.toString();
    }
}
