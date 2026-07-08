package io.quarkus.core.impl;

import java.util.ArrayDeque;

import org.jboss.logging.Logger;

import io.quarkus.runtime.ShutdownContext;

/**
 * Per-node {@link ShutdownContext} for legacy bytecode recorder compatibility.
 * <p>
 * Legacy recorders obtain a {@code ShutdownContext} via recorder proxy and
 * register shutdown tasks on it. This class collects those tasks and
 * implements {@link Runnable} so it can be registered as the service
 * node's stop handler via {@link ServiceNode#onStop(Runnable)}.
 * <p>
 * Tasks are executed in LIFO order (matching the behavior of the
 * original {@code StartupContext} shutdown mechanism). "Last" tasks
 * run after all normal tasks. Each task list is snapshot and cleared
 * atomically before execution to ensure safe concurrent access.
 *
 * @see ServiceNode#onStop(Runnable)
 */
public final class NodeShutdownContext implements ShutdownContext, Runnable {

    private static final Logger LOG = Logger.getLogger("io.quarkus.service");

    /** Normal shutdown tasks, stored in LIFO order (addFirst). */
    private final ArrayDeque<Runnable> tasks = new ArrayDeque<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addShutdownTask(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Shutdown task must not be null");
        }
        synchronized (tasks) {
            tasks.addFirst(runnable);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addLastShutdownTask(Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("Last shutdown task must not be null");
        }
        // delegate to the global collector — "last" tasks run during the
        // static-init stop cascade, after all runtime services and Arc
        // bean destruction have completed
        LastShutdownTasks.add(runnable);
    }

    /**
     * Execute all registered shutdown tasks in LIFO order.
     * Normal tasks run first, then last-priority tasks.
     * Each task list is snapshot and cleared before execution.
     * Exceptions are logged and do not prevent subsequent tasks from running.
     */
    @Override
    public void run() {
        runSnapshot(snapshotAndClear(tasks));
    }

    /**
     * Atomically snapshot and clear a task deque.
     *
     * @param deque the deque to snapshot (also used as the lock object)
     * @return the tasks as an array (in iteration order, which is LIFO)
     */
    private static Runnable[] snapshotAndClear(ArrayDeque<Runnable> deque) {
        synchronized (deque) {
            Runnable[] snapshot = deque.toArray(Runnable[]::new);
            deque.clear();
            return snapshot;
        }
    }

    /**
     * Run all tasks in the snapshot, logging any failures.
     *
     * @param snapshot the task array to execute
     */
    private static void runSnapshot(Runnable[] snapshot) {
        for (Runnable task : snapshot) {
            try {
                task.run();
            } catch (Throwable t) {
                LOG.error("Running a shutdown task failed", t);
            }
        }
    }
}
