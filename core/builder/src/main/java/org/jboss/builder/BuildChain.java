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
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import org.wildfly.common.Assert;

/**
 * A build chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildChain {
    private final Set<ItemId> initialIds;
    private final int initialSingleCount;
    private final int initialMultiCount;
    private final Set<ItemId> finalIds;
    private final List<StepInfo> startSteps;
    private final Set<ItemId> consumed;
    private final List<BuildProvider> providers;
    private final int endStepCount;

    BuildChain(final int initialSingleCount, final int initialMultiCount, final Set<StepInfo> startSteps,
            final Set<ItemId> consumed, BuildChainBuilder builder, final int endStepCount) {
        providers = builder.getProviders();
        initialIds = builder.getInitialIds();
        finalIds = builder.getFinalIds();
        this.initialSingleCount = initialSingleCount;
        this.initialMultiCount = initialMultiCount;
        this.startSteps = new ArrayList<>(startSteps);
        this.consumed = consumed;
        this.endStepCount = endStepCount;
    }

    /**
     * Create a new execution builder for this build chain.
     *
     * @param name the name of the build target for diagnostic purposes (must not be {@code null})
     * @return the new build execution builder (not {@code null})
     */
    public BuildExecutionBuilder createExecutionBuilder(String name) {
        final BuildExecutionBuilder builder = new BuildExecutionBuilder(this, name);
        for (BuildProvider provider : providers) {
            provider.prepareExecution(builder);
        }
        return builder;
    }

    /**
     * Get a new build chain builder.
     *
     * @return the build chain builder (not {@code null})
     */
    public static BuildChainBuilder builder() {
        return new BuildChainBuilder();
    }

    /**
     * Construct a build chain with the given name from providers found in the given class loader.
     *
     * @param classLoader the class loader to use
     * @return the build chain (not {@code null})
     * @throws ChainBuildException if building the chain failed
     */
    static BuildChain fromProviders(ClassLoader classLoader) throws ChainBuildException {
        final ArrayList<BuildProvider> list = new ArrayList<>();
        final ServiceLoader<BuildProvider> serviceLoader = ServiceLoader.load(BuildProvider.class, classLoader);
        for (final BuildProvider provider : serviceLoader) {
            list.add(provider);
        }
        return fromProviders(list);
    }

    /**
     * Construct a deployment chain with the given name from the given providers.
     *
     * @param providers the providers to use (must not be {@code null})
     * @return the deployment chain (not {@code null})
     * @throws ChainBuildException if building the chain failed
     */
    static BuildChain fromProviders(Collection<BuildProvider> providers) throws ChainBuildException {
        Assert.checkNotNullParam("providers", providers);
        final BuildChainBuilder builder = BuildChain.builder();
        for (BuildProvider provider : providers) {
            builder.addProvider(provider);
        }
        return builder.build();
    }

    boolean hasInitial(final ItemId itemId) {
        return initialIds.contains(itemId);
    }

    int getInitialSingleCount() {
        return initialSingleCount;
    }

    int getInitialMultiCount() {
        return initialMultiCount;
    }

    List<StepInfo> getStartSteps() {
        return startSteps;
    }

    Set<ItemId> getConsumed() {
        return consumed;
    }

    Set<ItemId> getFinalIds() {
        return finalIds;
    }

    int getEndStepCount() {
        return endStepCount;
    }
}
