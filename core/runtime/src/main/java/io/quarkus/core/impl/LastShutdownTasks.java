package io.quarkus.core.impl;

import java.util.ArrayDeque;

import org.jboss.logging.Logger;

/**
 * Global collector for "last" shutdown tasks registered via
 * {@link io.quarkus.runtime.ShutdownContext#addLastShutdownTask(Runnable)}.
 * <p>
 * Tasks are collected during application startup and executed during
 * the static-init graph's stop cascade, after all runtime services and
 * Arc bean destruction have completed. This preserves the original
 * "runs last" semantics that the per-node {@code NodeShutdownContext}
 * cannot provide.
 * <p>
 * A static-init service in the deployment module registers a stop handler
 * that calls {@link #run()} to execute all collected tasks.
 *
 * @see NodeShutdownContext
 */
public final class LastShutdownTasks {

    private static final Logger LOG = Logger.getLogger("io.quarkus.service");

    private static final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

    private LastShutdownTasks() {
    }

    /**
     * Add a task to the global "last" shutdown list.
     * Called by {@link NodeShutdownContext#addLastShutdownTask(Runnable)}.
     *
     * @param task the shutdown task (must not be {@code null})
     */
    public static void add(Runnable task) {
        synchronized (tasks) {
            tasks.addFirst(task);
        }
    }

    /**
     * Execute all registered tasks in LIFO order, then clear the list.
     * Called by the static-init cleanup service's stop handler.
     */
    public static void run() {
        Runnable[] snapshot;
        synchronized (tasks) {
            snapshot = tasks.toArray(Runnable[]::new);
            tasks.clear();
        }
        for (Runnable task : snapshot) {
            try {
                task.run();
            } catch (Throwable t) {
                LOG.error("Running a last shutdown task failed", t);
            }
        }
    }
}
