package io.quarkus.arc.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

public class InvokerCleanupTasks implements Consumer<Runnable> {
    private static final Logger LOG = Logger.getLogger(InvokerCleanupTasks.class);

    private final Set<Runnable> finishTasks = ConcurrentHashMap.newKeySet();

    @Override
    public void accept(Runnable task) {
        if (task != null) {
            finishTasks.add(task);
        }
    }

    // the generated invokers rely on this method not throwing
    public void finish() {
        for (Runnable task : finishTasks) {
            try {
                task.run();
            } catch (Exception e) {
                if (LOG.isDebugEnabled()) {
                    LOG.error("Exception thrown by an invoker cleanup task", e);
                } else {
                    LOG.error("Exception thrown by an invoker cleanup task: " + e);
                }
            }
        }
        finishTasks.clear();
    }
}
