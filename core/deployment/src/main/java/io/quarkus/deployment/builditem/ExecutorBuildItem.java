package io.quarkus.deployment.builditem;

import java.util.concurrent.ScheduledExecutorService;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * The main executor for blocking tasks.
 */
public final class ExecutorBuildItem extends SimpleBuildItem {

    private final ScheduledExecutorService executor;

    public ExecutorBuildItem(ScheduledExecutorService executor) {
        this.executor = executor;
    }

    public ScheduledExecutorService getExecutorProxy() {
        return executor;
    }
}
