package io.quarkus.deployment.builditem;

import org.wildfly.common.Assert;

import io.quarkus.builder.item.MultiBuildItem;
import io.quarkus.runtime.execution.ExecutionHandler;

/**
 * A build item for execution handlers.
 */
public final class ExecutionHandlerBuildItem extends MultiBuildItem {
    private final ExecutionHandler handler;

    /**
     * Construct a new instance.
     *
     * @param handler the handler (must not be {@code null})
     */
    public ExecutionHandlerBuildItem(final ExecutionHandler handler) {
        this.handler = Assert.checkNotNullParam("handler", handler);
    }

    /**
     * Get the execution handler.
     *
     * @return the execution handler (not {@code null})
     */
    public ExecutionHandler getHandler() {
        return handler;
    }
}
