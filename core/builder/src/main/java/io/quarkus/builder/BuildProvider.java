package io.quarkus.builder;

import java.util.ServiceLoader;

/**
 * A provider of deployers which can be detected via {@link ServiceLoader}.
 */
public interface BuildProvider {

    /**
     * Install this provider's deployers in to the given chain builder.
     *
     * @param builder the deployer chain builder (not {@code null})
     * @throws ChainBuildException if the installation fails for any reason
     */
    void installInto(BuildChainBuilder builder) throws ChainBuildException;

    /**
     * Run any preparatory steps required for a given deployment execution. This may include providing initial
     * resource values.
     *
     * @param builder the deployment execution builder (not {@code null})
     */
    default void prepareExecution(BuildExecutionBuilder builder) {
        // do nothing
    }
}
