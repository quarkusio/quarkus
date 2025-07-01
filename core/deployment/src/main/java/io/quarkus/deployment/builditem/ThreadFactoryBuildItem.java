package io.quarkus.deployment.builditem;

import java.util.concurrent.ThreadFactory;

import io.quarkus.builder.item.SimpleBuildItem;

/**
 * holds a {@link ThreadFactory} instance, used to configure thread creation for the main executor
 */
public final class ThreadFactoryBuildItem extends SimpleBuildItem {
    private final ThreadFactory threadFactory;

    public ThreadFactoryBuildItem(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
}