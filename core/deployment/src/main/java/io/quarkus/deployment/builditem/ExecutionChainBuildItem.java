package io.quarkus.deployment.builditem;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.SimpleBuildItem;
import io.quarkus.runtime.execution.ExecutionChain;

/**
 * A build item to hold the execution chain.
 */
public final class ExecutionChainBuildItem extends SimpleBuildItem {
    private final ExecutionChain chain;

    /**
     * Construct a new instance.
     *
     * @param chain the execution chain (must not be {@code null})
     */
    public ExecutionChainBuildItem(final ExecutionChain chain) {
        Assert.checkNotNullParam("chain", chain);
        this.chain = chain;
    }

    /**
     * Get the execution chain.
     *
     * @return the execution chain
     */
    public ExecutionChain getChain() {
        return chain;
    }
}
