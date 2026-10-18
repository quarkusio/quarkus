package io.quarkus.deployment.builditem;

import org.jboss.threads.ContextHandler;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * Holds a context handler used to propagate context when executing tasks.
 */
public final class ContextHandlerBuildItem extends SimpleBuildItem {
    private final ContextHandler<Object> contextHandler;

    public ContextHandlerBuildItem(ContextHandler<Object> contextHandler) {
        this.contextHandler = contextHandler;
    }

    public ContextHandler<Object> contextHandler() {
        return contextHandler;
    }
}
