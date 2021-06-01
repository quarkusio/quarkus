package io.quarkus.deployment.builditem;

import java.util.concurrent.ThreadFactory;

import io.quarkus.builder.item.SimpleBuildItem;

public final class ThreadFactoryBuildItem extends SimpleBuildItem {
    private final ThreadFactory threadFactory;

    public ThreadFactoryBuildItem(ThreadFactory threadFactory) {
        this.threadFactory = threadFactory;
    }

    public ThreadFactory getThreadFactory() {
        return threadFactory;
    }
}
