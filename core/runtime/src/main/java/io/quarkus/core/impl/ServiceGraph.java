package io.quarkus.core.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.logging.Logger;

import io.quarkus.runtime.StartupContext;

/**
 * The runtime service startup graph.
 * <p>
 * Manages the lifecycle of a set of {@link ServiceNode}s connected by
 * dependency edges. The graph uses two sentinel nodes:
 * <ul>
 * <li><b>Top sentinel:</b> has no dependencies; all root services
 * depend on it. When the graph starts, the top sentinel is
 * submitted to the executor; its completion triggers all root
 * services.</li>
 * <li><b>Bottom sentinel:</b> depends on all leaf services.
 * When all leaves complete, the bottom sentinel becomes ready;
 * its action signals that startup is done. During stop, its
 * stop is initiated first, cascading backward through the
 * graph.</li>
 * </ul>
 * <p>
 * <b>Usage:</b>
 * <ol>
 * <li>Create the graph, then create sentinel and service nodes
 * in topological order (top sentinel first, bottom sentinel
 * last), passing this graph to each constructor.</li>
 * <li>Call {@link #setTop(ServiceNode)} and
 * {@link #setBottom(ServiceNode)} to register the sentinels.</li>
 * <li>Call {@link #start()} to execute the graph. The calling
 * thread acts as the initial task executor, draining a
 * built-in task queue.</li>
 * <li>Call {@link #stop()} to tear down the graph in reverse
 * dependency order.</li>
 * </ol>
 * <p>
 * Call {@link #setExecutor(Executor)} to upgrade to a thread pool
 * mid-startup (typically during the {@code INFRASTRUCTURE} phase);
 * this returns the previous executor for later restoration during
 * shutdown.
 *
 * @see ServiceNode
 */
public final class ServiceGraph {

    private static final Logger LOG = Logger.getLogger("io.quarkus.service");

    private static final VarHandle EXECUTOR_HANDLE = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "executor", VarHandle.class, ServiceGraph.class, Executor.class);
    private static final VarHandle TOP_HANDLE = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "top", VarHandle.class, ServiceGraph.class, ServiceNode.class);
    private static final VarHandle BOTTOM_HANDLE = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "bottom", VarHandle.class, ServiceGraph.class, ServiceNode.class);
    private static final VarHandle FIRST_FAILURE_HANDLE = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "firstFailure", VarHandle.class, ServiceGraph.class, Throwable.class);

    // --- main-thread task queue ---

    /** Lock protecting the task queue and done conditions. */
    private final ReentrantLock lock = new ReentrantLock();

    /** Signaled when a task is submitted or a done flag changes. */
    private final Condition taskAvailable = lock.newCondition();

    /** Task queue for the main-thread executor. */
    private final ArrayDeque<Runnable> queue = new ArrayDeque<>();

    /**
     * The main-thread executor. Submits tasks to the internal queue
     * and signals the draining thread.
     */
    private final Executor mainThreadExecutor = this::submitToMainThread;

    // --- graph endpoints (set-once via CAS) ---

    /** Top sentinel node. Set once via {@link #setTop(ServiceNode)}. */
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile ServiceNode top;

    /** Bottom sentinel node. Set once via {@link #setBottom(ServiceNode)}. */
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile ServiceNode bottom;

    // --- executor ---

    /** The current executor. Initially the main-thread executor. */
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile Executor executor;

    // --- done flags ---

    /** Set by the bottom sentinel's action to signal startup completion. */
    private volatile boolean startDone;

    /** Set by the top sentinel's stop handler to signal stop completion. */
    private volatile boolean stopDone;

    /** The first failure encountered during startup, if any. */
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile Throwable firstFailure;

    /**
     * Shared startup context for legacy bytecode recorder compatibility.
     * Legacy recorders read and write values through this context.
     * Must be concurrent-safe (maps replaced with ConcurrentHashMap).
     */
    private final StartupContext startupContext;

    /**
     * Construct a new service graph.
     * The graph starts with the main-thread executor. Sentinel nodes
     * must be registered via {@link #setTop(ServiceNode)} and
     * {@link #setBottom(ServiceNode)} before calling {@link #start()}.
     */
    /**
     * Construct a new service graph with a fresh startup context.
     */
    public ServiceGraph() {
        this(new StartupContext());
    }

    /**
     * Construct a new service graph with the given startup context.
     *
     * @param startupContext the startup context for legacy recorder compatibility
     *        (must not be {@code null})
     */
    public ServiceGraph(StartupContext startupContext) {
        this.executor = mainThreadExecutor;
        this.startupContext = startupContext;
    }

    // --- sentinel setters (set-once) ---

    /**
     * Set the top sentinel node. Must be called exactly once.
     * The top sentinel has no dependencies; all root services should
     * depend on it.
     *
     * @param top the top sentinel (must not be {@code null})
     * @throws IllegalStateException if already set
     */
    public void setTop(ServiceNode top) {
        if (TOP_HANDLE.compareAndExchangeRelease(this, null, top) != null) {
            throw new IllegalStateException("Top sentinel already set");
        }
    }

    /**
     * Set the bottom sentinel node. Must be called exactly once.
     * The bottom sentinel should depend on all leaf services.
     *
     * @param bottom the bottom sentinel (must not be {@code null})
     * @throws IllegalStateException if already set
     */
    public void setBottom(ServiceNode bottom) {
        if (BOTTOM_HANDLE.compareAndExchangeRelease(this, null, bottom) != null) {
            throw new IllegalStateException("Bottom sentinel already set");
        }
    }

    // --- executor ---

    /**
     * Get the current executor.
     *
     * @return the current executor (never {@code null})
     */
    Executor executor() {
        return (Executor) EXECUTOR_HANDLE.getVolatile(this);
    }

    /**
     * Set a new executor for dispatching service actions.
     * Returns the previous executor so it can be restored later
     * (typically during shutdown).
     *
     * @param newExecutor the new executor (must not be {@code null})
     * @return the previous executor
     */
    public Executor setExecutor(Executor newExecutor) {
        return (Executor) EXECUTOR_HANDLE.getAndSet(this, newExecutor);
    }

    /**
     * Get the shared startup context for legacy recorder compatibility.
     *
     * @return the startup context (never {@code null})
     */
    public StartupContext startupContext() {
        return startupContext;
    }

    // --- signaling (called by sentinel actions/stop handlers) ---

    /**
     * Record a startup failure. The first failure becomes the primary
     * exception; subsequent failures are added as suppressed exceptions
     * on the primary. Called by {@link ServiceNode#propagateFailure}.
     *
     * @param cause the failure cause
     */
    void recordFailure(Throwable cause) {
        Throwable existing = (Throwable) FIRST_FAILURE_HANDLE.compareAndExchange(this, null, cause);
        if (existing != null && existing != cause) {
            existing.addSuppressed(cause);
        }
    }

    /**
     * Signal that startup is complete. Called by the bottom sentinel's
     * action when it runs (indicating all leaf services have completed).
     */
    public void signalStartDone() {
        startDone = true;
        signalMainThread();
    }

    /**
     * Signal that stop is complete. Called by the top sentinel's stop
     * handler when it runs (indicating all services have been torn down).
     */
    public void signalStopDone() {
        stopDone = true;
        signalMainThread();
    }

    // --- start ---

    /**
     * Start the service graph.
     * <p>
     * Submits the top sentinel to the executor, which cascades to all
     * root services. Drains the main-thread task queue until the bottom
     * sentinel signals startup completion.
     * <p>
     * If any service fails, the failure cascades forward (canceling
     * dependents) and backward (stopping completed services). The bottom
     * sentinel's action still runs, signaling start completion, after
     * which this method returns. The caller should inspect the graph
     * for failures.
     *
     * @throws RuntimeException if interrupted while waiting
     */
    public void start() {
        ServiceNode top = (ServiceNode) TOP_HANDLE.getVolatile(this);
        if (top == null) {
            throw new IllegalStateException("Top sentinel not set");
        }
        ServiceNode bottom = (ServiceNode) BOTTOM_HANDLE.getVolatile(this);
        if (bottom == null) {
            throw new IllegalStateException("Bottom sentinel not set");
        }
        executor().execute(top);
        // drain until the bottom sentinel either completes (signaling startDone)
        // or is canceled/failed (failure propagated to sentinel)
        drainUntil(() -> startDone || bottom.state() >= ServiceNode.S_FAILED);
        // rethrow the first failure so callers see it as if it were thrown directly
        Throwable failure = (Throwable) FIRST_FAILURE_HANDLE.getVolatile(this);
        if (failure != null) {
            if (failure instanceof RuntimeException re) {
                throw re;
            }
            if (failure instanceof Error e) {
                throw e;
            }
            throw new RuntimeException(failure);
        }
    }

    /**
     * Stop the service graph.
     * <p>
     * Initiates the stop cascade from the bottom sentinel and drains
     * the main-thread task queue until the top sentinel's stop handler
     * signals stop completion.
     */
    public void stop() {
        ServiceNode bottom = (ServiceNode) BOTTOM_HANDLE.getVolatile(this);
        if (bottom == null) {
            return; // empty graph (no services were registered)
        }
        ServiceNode top = (ServiceNode) TOP_HANDLE.getVolatile(this);
        bottom.initiateStop();
        // drain until the top sentinel's stop handler signals stopDone
        // or the top is already in a terminal state (failure during start)
        drainUntil(() -> stopDone || top.state() >= ServiceNode.S_STOPPED);
        // disconnect the node graph so that the MethodHandle action fields
        // (which pin QuarkusClassLoader-loaded classes) become unreachable
        TOP_HANDLE.setRelease(this, null);
        BOTTOM_HANDLE.setRelease(this, null);
        // eagerly release the StartupContext maps
        startupContext.close();
    }

    // --- main-thread task queue ---

    /**
     * Submit a task to the main-thread queue.
     *
     * @param task the task to submit
     */
    private void submitToMainThread(Runnable task) {
        lock.lock();
        try {
            queue.add(task);
            taskAvailable.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Signal the main thread to wake up and re-check conditions.
     * Called by {@link ServiceNode#dependencyFailed} when a node is
     * canceled, in case the canceled node is a sentinel whose action
     * would otherwise have signaled completion.
     */
    void signalMainThread() {
        lock.lock();
        try {
            taskAvailable.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Drain the main-thread task queue until the given condition is met.
     *
     * @param done the condition to check
     */
    private void drainUntil(java.util.function.BooleanSupplier done) {
        lock.lock();
        try {
            while (!done.getAsBoolean()) {
                Runnable task = queue.poll();
                if (task == null) {
                    taskAvailable.await();
                } else {
                    lock.unlock();
                    try {
                        task.run();
                    } finally {
                        lock.lock();
                    }
                }
            }
            // flush any remaining tasks (nodes submitted before the condition was met
            // but not yet processed — e.g. services submitted before a failure cascade
            // canceled the sentinel)
            Runnable remaining;
            while ((remaining = queue.poll()) != null) {
                lock.unlock();
                try {
                    remaining.run();
                } finally {
                    lock.lock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted during service graph execution", e);
        } finally {
            lock.unlock();
        }
    }
}
