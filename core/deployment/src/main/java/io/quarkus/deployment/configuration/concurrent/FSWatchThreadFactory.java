package io.quarkus.deployment.configuration.concurrent;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class FSWatchThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber = new AtomicInteger(0);
    private final String threadNamePrefix;

    public FSWatchThreadFactory(String threadNamePrefix) {
        this.threadNamePrefix = threadNamePrefix;
    }

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, String.format(threadNamePrefix, threadNumber.addAndGet(1)));
    }
}
