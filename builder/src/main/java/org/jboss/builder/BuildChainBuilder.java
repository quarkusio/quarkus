/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import org.jboss.builder.item.BuildItem;
import org.jboss.builder.item.SymbolicBuildItem;

/**
 * A build chain builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildChainBuilder {

    private final BuildStepBuilder finalStep;
    private final List<BuildProvider> providers = new ArrayList<>();
    private final Map<BuildStepBuilder, StackTraceElement[]> steps = new HashMap<BuildStepBuilder, StackTraceElement[]>();
    private final Set<ItemId> initialIds = new HashSet<>();
    private final Set<ItemId> finalIds = new HashSet<>();

    BuildChainBuilder() {
        finalStep = addBuildStep(new FinalStep());
    }

    /**
     * Add a build step to the chain.  The configuration in the build step builder at the time that the chain is built is
     * the configuration that will apply to the build step in the final chain.  Any subsequent changes will be ignored.
     * <p>
     * A given build step is included in the chain when one or more of the following criteria are met:
     * <ul>
     * <li>It includes a pre-produce step for a item which is produced by one or more build steps that is included in the chain</li>
     * <li>It includes a produce step for a item which is consumed by a build step that is included in the chain or is a final item</li>
     * <li>It includes a consume step for a item which is produced by a build step that is included in the chain or is an initial item</li>
     * <li>It includes a destroy step for a item which is produced by a build step that is included in the chain or is an initial item</li>
     * </ul>
     * In addition, the declaration of producers and consumers can cause corresponding consumers and producers to be
     * included if they exist.
     *
     * @param buildStep the build step instance
     * @return the builder for the build step
     */
    public BuildStepBuilder addBuildStep(BuildStep buildStep) {
        final BuildStepBuilder buildStepBuilder = new BuildStepBuilder(this);
        buildStepBuilder.setBuildStep(buildStep);
        return buildStepBuilder;
    }

    /**
     * Add a build step to the chain.  The configuration in the build step builder at the time that the chain is built is
     * the configuration that will apply to the build step in the final chain.  Any subsequent changes will be ignored.
     * <p>
     * A given build step is included in the chain when one or more of the following criteria are met:
     * <ul>
     * <li>It includes a pre-produce step for a item which is produced by one or more build steps that is included in the chain</li>
     * <li>It includes a produce step for a item which is consumed by a build step that is included in the chain or is a final item</li>
     * <li>It includes a consume step for a item which is produced by a build step that is included in the chain or is an initial item</li>
     * <li>It includes a destroy step for a item which is produced by a build step that is included in the chain or is an initial item</li>
     * </ul>
     * In addition, the declaration of producers and consumers can cause corresponding consumers and producers to be
     * included if they exist.
     *
     * @return the builder for the build step
     */
    public BuildStepBuilder addBuildStep() {
        return new BuildStepBuilder(this);
    }

    /**
     * Declare an initial item that will be provided to build steps in the chain.  Note that if this method is called
     * for a simple item, no build steps will be allowed to produce that item.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the item type is {@code null}
     */
    public BuildChainBuilder addInitial(Class<? extends BuildItem> type) {
        initialIds.add(new ItemId(type, null));
        return this;
    }

    public BuildChainBuilder addInitial(Enum<?> symbolic) {
        initialIds.add(new ItemId(SymbolicBuildItem.class, symbolic));
        return this;
    }

    public BuildChainBuilder loadProviders(ClassLoader classLoader) throws ChainBuildException {
        final ArrayList<BuildProvider> list = new ArrayList<>();
        final ServiceLoader<BuildProvider> serviceLoader = ServiceLoader.load(BuildProvider.class, classLoader);
        for (final BuildProvider provider : serviceLoader) {
            provider.installInto(this);
        }
        return this;
    }
    /**
     * Declare a final item that will be consumable after the build step chain completes.  This may be any item
     * that is produced in the chain.
     *
     * @param type the item type (must not be {@code null})
     * @return this builder
     * @throws IllegalArgumentException if the item type is {@code null}
     */
    public BuildChainBuilder addFinal(Class<? extends BuildItem> type) {
        finalIds.add(new ItemId(type, null));
        return this;
    }

    public BuildChainBuilder addFinal(Enum<?> symbolic) {
        finalIds.add(new ItemId(SymbolicBuildItem.class, symbolic));
        return this;
    }

    /**
     * Build the build step chain from the current builder configuration.
     *
     * @return the constructed build  chain
     * @throws ChainBuildException if the chain could not be built
     */
    public BuildChain build() throws ChainBuildException {
        final Set<ItemId> consumed = new HashSet<>();
        final List<StepInfo> startSteps = new ArrayList<>();
        final Map<BuildStepBuilder, StepInfo> mappedSteps = new HashMap<>();
        int initialSingleCount = 0;
        int initialMultiCount = 0;
        final Map<BuildStepBuilder, StackTraceElement[]> steps = this.steps;
        // compile master produce/consume maps
        final Map<ItemId, List<Consume>> allConsumes = new HashMap<>();
        final Map<ItemId, List<Produce>> allProduces = new HashMap<>();
        final Set<ItemId> initialIds = this.initialIds;
        for (Map.Entry<BuildStepBuilder, StackTraceElement[]> stepEntry : steps.entrySet()) {
            final BuildStepBuilder stepBuilder = stepEntry.getKey();
            final Map<ItemId, Consume> stepConsumes = stepBuilder.getConsumes();
            for (Map.Entry<ItemId, Consume> entry : stepConsumes.entrySet()) {
                final ItemId id = entry.getKey();
                final List<Consume> list = allConsumes.computeIfAbsent(id, x -> new ArrayList<>(4));
                list.add(entry.getValue());
            }
            final Map<ItemId, Produce> stepProduces = stepBuilder.getProduces();
            for (Map.Entry<ItemId, Produce> entry : stepProduces.entrySet()) {
                final ItemId id = entry.getKey();
                final List<Produce> list = allProduces.computeIfAbsent(id, x -> new ArrayList<>(2));
                if (! id.isMulti() && entry.getValue().getConstraint() == Constraint.REAL) {
                    // ensure only one producer
                    if (initialIds.contains(id)) {
                        final ChainBuildException cbe = new ChainBuildException("Item " + id + " cannot be produced here (it is an initial resource)");
                        cbe.setStackTrace(steps.get(entry.getValue().getStepBuilder()));
                        throw cbe;
                    }
                    for (Produce produce : list) {
                        if (produce.getConstraint() == Constraint.REAL) {
                            final Throwable cause = new Throwable("This is the location of the conflicting producer");
                            cause.setStackTrace(steps.get(entry.getValue().getStepBuilder()));
                            final ChainBuildException cbe = new ChainBuildException("Multiple producers of item " + id, cause);
                            cbe.setStackTrace(steps.get(produce.getStepBuilder()));
                            throw cbe;
                        }
                    }
                }
                list.add(entry.getValue());
            }
        }
        final Set<BuildStepBuilder> included = new HashSet<>();
        // now begin to wire dependencies
        final Set<ItemId> finalIds = this.finalIds;
        final ArrayDeque<BuildStepBuilder> toAdd = new ArrayDeque<>();
        final Set<BuildStepBuilder> lastDependencies = new HashSet<>();
        for (ItemId finalId : finalIds) {
            addOne(allProduces, included, toAdd, finalId, lastDependencies);
        }
        // now recursively add producers of consumed items
        BuildStepBuilder stepBuilder;
        Map<BuildStepBuilder, Set<BuildStepBuilder>> dependencies = new HashMap<>();
        while ((stepBuilder = toAdd.pollFirst()) != null) {
            for (Map.Entry<ItemId, Consume> entry : stepBuilder.getConsumes().entrySet()) {
                final Consume consume = entry.getValue();
                final ItemId id = entry.getKey();
                if (! consume.getFlags().contains(ConsumeFlag.OPTIONAL) && !id.isMulti()) {
                    if (! initialIds.contains(id) && ! allProduces.containsKey(id)) {
                        throw new ChainBuildException("No producers for required item " + id);
                    }
                }
                // add every producer
                addOne(allProduces, included, toAdd, id, dependencies.computeIfAbsent(stepBuilder, x -> new HashSet<>()));
            }
        }
        // calculate dependents
        Map<BuildStepBuilder, Set<BuildStepBuilder>> dependents = new HashMap<>();
        for (Map.Entry<BuildStepBuilder, Set<BuildStepBuilder>> entry : dependencies.entrySet()) {
            final BuildStepBuilder dependent = entry.getKey();
            for (BuildStepBuilder dependency : entry.getValue()) {
                dependents.computeIfAbsent(dependency, x -> new HashSet<>()).add(dependent);
            }
        }
        // detect cycles
        cycleCheck(included, new HashSet<>(), new HashSet<>(), dependencies);
        // recursively build all
        List<StepInfo> endSteps = new ArrayList<>();
        for (BuildStepBuilder builder : included) {
            buildOne(builder, mappedSteps, dependents, dependencies, startSteps, endSteps);
        }
        return new BuildChain(initialSingleCount, initialMultiCount, startSteps, consumed, this, endSteps.size());
    }

    private void cycleCheck(Set<BuildStepBuilder> builders, Set<BuildStepBuilder> visited, Set<BuildStepBuilder> checked, final Map<BuildStepBuilder, Set<BuildStepBuilder>> dependencies) throws ChainBuildException {
        for (BuildStepBuilder builder : builders) {
            cycleCheck(builder, visited, checked, dependencies);
        }
    }

    private void cycleCheck(BuildStepBuilder builder, Set<BuildStepBuilder> visited, Set<BuildStepBuilder> checked, final Map<BuildStepBuilder, Set<BuildStepBuilder>> dependencies) throws ChainBuildException {
        if (! checked.contains(builder)) {
            if (! visited.add(builder)) {
                throw new ChainBuildException("Cycle detected: " + visited);
            }
            try {
                cycleCheck(dependencies.getOrDefault(builder, Collections.emptySet()), visited, checked, dependencies);
            } finally {
                visited.remove(builder);
            }
        }
        checked.add(builder);
    }

    private void addOne(final Map<ItemId, List<Produce>> allProduces, final Set<BuildStepBuilder> included, final ArrayDeque<BuildStepBuilder> toAdd, final ItemId idToAdd, Set<BuildStepBuilder> dependencies) throws ChainBuildException {
        for (Produce produce : allProduces.getOrDefault(idToAdd, Collections.emptyList())) {
            final BuildStepBuilder stepBuilder = produce.getStepBuilder();
            if (included.add(stepBuilder)) {
                // recursively add
                toAdd.addLast(stepBuilder);
            }
            dependencies.add(stepBuilder);
        }
    }

    private StepInfo buildOne(BuildStepBuilder toBuild, Map<BuildStepBuilder, StepInfo> mapped, Map<BuildStepBuilder, Set<BuildStepBuilder>> dependents, Map<BuildStepBuilder, Set<BuildStepBuilder>> dependencies, final List<StepInfo> startSteps, final List<StepInfo> endSteps) {
        if (mapped.containsKey(toBuild)) {
            return mapped.get(toBuild);
        }
        Set<StepInfo> dependentStepInfos = new HashSet<>();
        final Set<BuildStepBuilder> dependentsOfThis = dependents.getOrDefault(toBuild, Collections.emptySet());
        for (BuildStepBuilder dependentBuilder : dependentsOfThis) {
            dependentStepInfos.add(buildOne(dependentBuilder, mapped, dependents, dependencies, startSteps, endSteps));
        }
        final Set<BuildStepBuilder> dependenciesOfThis = dependencies.getOrDefault(toBuild, Collections.emptySet());
        final StepInfo stepInfo = new StepInfo(toBuild, dependenciesOfThis.size(), dependentStepInfos);
        mapped.put(toBuild, stepInfo);
        if (dependenciesOfThis.isEmpty()) {
            // it's a start step!
            startSteps.add(stepInfo);
        }
        if (dependentsOfThis.isEmpty()) {
            // it's an end step!
            endSteps.add(stepInfo);
        }
        return stepInfo;
    }

    void addProvider(final BuildProvider provider) throws ChainBuildException {
        providers.add(provider);
        provider.installInto(this);
    }

    void addStep(final BuildStepBuilder stepBuilder, final StackTraceElement[] stackTrace) {
        if (stepBuilder.getBuildStep() == null) {
            throw new IllegalArgumentException("Null build step");
        }
        steps.put(stepBuilder, stackTrace);
    }

    BuildStepBuilder getFinalStep() {
        return finalStep;
    }

    List<BuildProvider> getProviders() {
        return providers;
    }

    Map<BuildStepBuilder, StackTraceElement[]> getSteps() {
        return steps;
    }

    Set<ItemId> getInitialIds() {
        return initialIds;
    }

    Set<ItemId> getFinalIds() {
        return finalIds;
    }
}
