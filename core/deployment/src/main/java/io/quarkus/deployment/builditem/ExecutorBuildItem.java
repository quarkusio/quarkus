package io.quarkus.deployment.builditem;

import java.util.concurrent.ExecutorService;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The main executor for blocking tasks
 */
public final class ExecutorBuildItem extends SimpleBuildItem {
    private final ExecutorService executor;

    public ExecutorBuildItem(final ExecutorService executor) {
        this.executor = executor;
    }

    public ExecutorService getExecutorProxy() {
        return executor;
    }
}
