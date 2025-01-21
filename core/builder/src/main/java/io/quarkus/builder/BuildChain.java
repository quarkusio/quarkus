package io.quarkus.builder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import io.smallrye.common.constraint.Assert;

/**
 * A build chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildChain {
    private final Set<ItemId> initialIds;
    private final Set<ItemId> finalIds;
    private final List<StepInfo> startSteps;
    private final List<BuildProvider> providers;
    private final int endStepCount;
    private final ClassLoader classLoader;

    BuildChain(final Set<StepInfo> startSteps, BuildChainBuilder builder, final int endStepCount) {
        providers = builder.getProviders();
        initialIds = builder.getInitialIds();
        finalIds = builder.getFinalIds();
        this.startSteps = new ArrayList<>(startSteps);
        this.endStepCount = endStepCount;
        this.classLoader = builder.getClassLoader();
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

    List<StepInfo> getStartSteps() {
        return startSteps;
    }

    Set<ItemId> getFinalIds() {
        return finalIds;
    }

    ClassLoader getClassLoader() {
        return classLoader;
    }

    int getEndStepCount() {
        return endStepCount;
    }
}
