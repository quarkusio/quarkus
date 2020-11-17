package io.quarkus.builder;

import java.util.List;

import io.quarkus.qlue.Chain;

/**
 * A build chain.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class BuildChain {
    private final List<BuildProvider> providers;
    private final Chain chain;

    BuildChain(final Chain chain, final List<BuildProvider> providers) {
        this.chain = chain;
        this.providers = providers;
    }

    /**
     * Create a new execution builder for this build chain.
     *
     * @param name the name of the build target for diagnostic purposes (must not be {@code null})
     * @return the new build execution builder (not {@code null})
     */
    public BuildExecutionBuilder createExecutionBuilder(String name) {
        final BuildExecutionBuilder builder = new BuildExecutionBuilder(chain.createExecutionBuilder(), name);
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
        return new BuildChainBuilder(Chain.builder());
    }

    /**
     * Get the real chain.
     *
     * @return the real chain (not {@code null})
     */
    public Chain getChain() {
        return chain;
    }
}
