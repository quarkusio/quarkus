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
 * that calls {@link #runStatic()} to execute all collected tasks.
 *
 * @see NodeShutdownContext
 */
public final class LastShutdownTasks {

    private static final Logger LOG = Logger.getLogger("io.quarkus.service");

    /**
     * The static-init "last" shutdown tasks.
     */
    private static final ArrayDeque<Runnable> staticTasks = new ArrayDeque<>();

    /**
     * The runtime-init "last" shutdown tasks.
     */
    private static final ArrayDeque<Runnable> runtimeTasks = new ArrayDeque<>();

    private LastShutdownTasks() {
    }

    /**
     * Add a task to the global static-init "last" shutdown list.
     * Called by {@link NodeShutdownContext#addLastShutdownTask(Runnable)}.
     *
     * @param task the shutdown task (must not be {@code null})
     */
    public static void addStatic(Runnable task) {
        synchronized (staticTasks) {
            staticTasks.addFirst(task);
        }
    }

    /**
     * Add a task to the global runtime-init "last" shutdown list.
     * Called by {@link NodeShutdownContext#addLastShutdownTask(Runnable)}.
     *
     * @param task the shutdown task (must not be {@code null})
     */
    public static void addRuntime(Runnable task) {
        synchronized (runtimeTasks) {
            runtimeTasks.addFirst(task);
        }
    }

    /**
     * Execute all registered static-init tasks in LIFO order, then clear the list.
     * Called by the static-init cleanup service's stop handler.
     */
    public static void runStatic() {
        runTasks(staticTasks);
    }

    /**
     * Execute all registered runtime-init tasks in LIFO order, then clear the list.
     * Called by the runtime-init cleanup service's stop handler.
     */
    public static void runRuntime() {
        runTasks(runtimeTasks);
    }

    /**
     * Run all tasks in the provided queue in LIFO order.
     *
     * @param queue the queue of tasks to execute
     */
    private static void runTasks(ArrayDeque<Runnable> queue) {
        Runnable[] snapshot;
        synchronized (queue) {
            snapshot = queue.toArray(Runnable[]::new);
            queue.clear();
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
