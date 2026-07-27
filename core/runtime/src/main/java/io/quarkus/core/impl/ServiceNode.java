package io.quarkus.core.impl;

import java.lang.invoke.ConstantBootstraps;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.core.AsyncStartContext;
import io.quarkus.core.AsyncStopContext;
import io.quarkus.core.AsyncVoidStartContext;

/**
 * A node in the service startup graph.
 * <p>
 * Each node represents a single service action (either a new-style service
 * or a legacy recorder chunk). The node is simultaneously:
 * <ul>
 * <li>the graph node (dependencies, dependents, value)</li>
 * <li>the runnable task (submitted to the executor when ready)</li>
 * <li>the start context for the action (stop handler registration, async completion)</li>
 * </ul>
 * <p>
 * Nodes must be constructed in topological order so that all dependencies
 * exist before their dependents are created. The constructor wires the
 * reverse (dependent) links automatically.
 * <p>
 * <b>Lifecycle:</b>
 * <ol>
 * <li><b>Construction</b> (single-threaded): nodes are created with their
 * dependency lists (final). Each constructor call registers this node
 * as a dependent of each dependency.</li>
 * <li><b>Start</b>: root nodes (those with no dependencies) are submitted
 * to the executor by {@link ServiceGraph#start()}. Each node's
 * {@link #run()} method invokes the action via {@link MethodHandle},
 * and the action calls {@link #startComplete(Object)} or
 * {@link #startComplete()} to signal success. Completion triggers
 * {@link #dependencySatisfied()} on each dependent. When a
 * dependent's remaining count hits zero, it submits itself to the
 * executor.</li>
 * <li><b>Stop</b>: reverse wave-front. When a node's live-dependent count
 * reaches zero and it is in the {@code COMPLETED} state, it runs its
 * stop handler. Upon stop completion, it notifies each dependency via
 * {@link #dependentTerminated()}, decrementing their live-dependent
 * counts and potentially triggering their stop in turn.</li>
 * </ol>
 * <p>
 * <b>State machine:</b>
 *
 * <pre>
 * PENDING → RUNNING → COMPLETED → STOPPING → STOPPED
 *                   ↘ FAILED
 * PENDING → CANCELED (a dependency failed before this node started)
 * </pre>
 * <p>
 * <b>Atomicity:</b>
 * The lifecycle state, remaining dependency count, and live dependent count
 * are packed into a single {@code long} field and updated atomically via
 * {@code compareAndExchange}. This eliminates races between concurrent state
 * transitions and counter updates — for example, a dependent terminating
 * (decrementing live-dependents) while the node is completing (transitioning
 * RUNNING → COMPLETED). With separate fields, these two operations could
 * interleave such that neither thread sees the combined condition (COMPLETED
 * + zero live-dependents) that should trigger the stop sequence. The packed
 * field makes the check-and-act atomic.
 *
 * @see ServiceGraph
 */
public final class ServiceNode implements Runnable, AsyncStartContext<Object>, AsyncVoidStartContext {

    private static final Logger LOG = Logger.getLogger("io.quarkus.service");

    // ═══════════════════════════════════════════════
    // State constants
    // ═══════════════════════════════════════════════

    /** Node has not yet started. */
    static final int S_PENDING = 0;
    /** Node's action is executing. */
    static final int S_RUNNING = 1;
    /** Node's action completed successfully. */
    static final int S_COMPLETED = 2;
    /** Node's action failed. */
    static final int S_FAILED = 3;
    /** Node was canceled because a dependency failed. */
    static final int S_CANCELED = 4;
    /** Node's stop handler is executing. */
    static final int S_STOPPING = 5;
    /** Node's stop handler completed. */
    static final int S_STOPPED = 6;

    // ═══════════════════════════════════════════════
    // Packed field layout
    // ═══════════════════════════════════════════════
    //
    // A single 64-bit long packs three fields into independent bit ranges.
    // All transitions use compareAndExchange on this single field, ensuring
    // that state checks and counter modifications are always atomic with
    // respect to each other. The witness value returned on failure is used
    // directly as the retry value, avoiding a redundant volatile read.
    //
    //   Bits  0–15 : state           (max 65535; only 7 values used)
    //   Bits 16–31 : remainingDeps   (max 65535 dependencies)
    //   Bits 32–47 : liveDependents  (max 65535 dependents)
    //   Bits 48–63 : reserved (zero)

    /** Bit width of each packed sub-field (16 bits → max 65535). */
    private static final long FIELD_MASK = 0xFFFFL;

    /** Shift for the remaining-dependencies count. */
    private static final int REMAINING_DEPS_SHIFT = 16;

    /** Shift for the live-dependents count. */
    private static final int LIVE_DEPENDENTS_SHIFT = 32;

    /**
     * Extract the state component from a packed value.
     *
     * @param packed the packed long value
     * @return the state constant (one of {@code S_*})
     */
    static int stateOf(long packed) {
        return (int) (packed & FIELD_MASK);
    }

    /**
     * Extract the remaining-dependencies count from a packed value.
     *
     * @param packed the packed long value
     * @return the number of unsatisfied dependencies
     */
    static int remainingDepsOf(long packed) {
        return (int) ((packed >>> REMAINING_DEPS_SHIFT) & FIELD_MASK);
    }

    /**
     * Extract the live-dependents count from a packed value.
     *
     * @param packed the packed long value
     * @return the number of dependents not yet in a terminal state
     */
    static int liveDependentsOf(long packed) {
        return (int) ((packed >>> LIVE_DEPENDENTS_SHIFT) & FIELD_MASK);
    }

    /**
     * Pack the three components into a single long value.
     *
     * @param state the state constant
     * @param remainingDeps the remaining-dependencies count
     * @param liveDependents the live-dependents count
     * @return the packed long
     */
    static long pack(int state, int remainingDeps, int liveDependents) {
        return ((long) state)
                | ((long) remainingDeps << REMAINING_DEPS_SHIFT)
                | ((long) liveDependents << LIVE_DEPENDENTS_SHIFT);
    }

    // ═══════════════════════════════════════════════
    // VarHandles for atomic field operations
    // ═══════════════════════════════════════════════

    /** VarHandle for the packed lifecycle field. */
    private static final VarHandle PACKED = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "packed", VarHandle.class, ServiceNode.class, long.class);

    /** VarHandle for the stop handler field. */
    private static final VarHandle STOP_HANDLER = ConstantBootstraps.fieldVarHandle(
            MethodHandles.lookup(), "stopHandler", VarHandle.class, ServiceNode.class, Consumer.class);

    // ═══════════════════════════════════════════════
    // Graph structure (set during single-threaded construction)
    // ═══════════════════════════════════════════════

    /** Diagnostic name for logging. */
    private final String name;

    /**
     * The action to invoke. Signature: {@code (ServiceNode) → void}.
     * The action must call {@link #startComplete(Object)}, {@link #startComplete()},
     * or {@link #startFailed(Throwable)} to signal its outcome.
     */
    private final MethodHandle action;

    /** Direct dependencies. Immutable after construction. */
    private final List<ServiceNode> dependencies;

    /** The owning graph (provides executor access). */
    private final ServiceGraph graph;

    /** Direct dependents. Built up during construction, used as-is during lifecycle. */
    private final ArrayList<ServiceNode> dependents = new ArrayList<>();

    // ═══════════════════════════════════════════════
    // Packed lifecycle state (atomic via PACKED VarHandle)
    // ═══════════════════════════════════════════════

    /**
     * Packed lifecycle state combining state, remaining dependency count,
     * and live dependent count in a single atomic field.
     */
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile long packed;

    // ═══════════════════════════════════════════════
    // Value and failure
    // ═══════════════════════════════════════════════

    /** The produced value (for typed services). Set before state → COMPLETED. */
    private volatile Object value;

    /** The failure cause. Set before state → FAILED. */
    private Throwable failure;

    // ═══════════════════════════════════════════════
    // Stop handler
    // ═══════════════════════════════════════════════

    /**
     * The stop handler registered via {@link #onStopAsync(Consumer)}.
     * At most one registration is allowed; subsequent attempts throw.
     */
    @SuppressWarnings("unused") // managed by VarHandle
    private volatile Consumer<AsyncStopContext> stopHandler;

    // ═══════════════════════════════════════════════
    // Constructors
    // ═══════════════════════════════════════════════

    /**
     * Construct a new node with no dependencies.
     *
     * @param name the diagnostic name (must not be {@code null})
     * @param action the action handle (must not be {@code null})
     * @param graph the owning graph (must not be {@code null})
     * @param dependentCount the number of dependents this node will have
     */
    public ServiceNode(String name, MethodHandle action, ServiceGraph graph, int dependentCount) {
        this(name, action, graph, dependentCount, List.of());
    }

    /**
     * Construct a new node with one dependency.
     *
     * @param name the diagnostic name (must not be {@code null})
     * @param action the action handle (must not be {@code null})
     * @param graph the owning graph (must not be {@code null})
     * @param dependentCount the number of dependents this node will have
     * @param dep the dependency (must not be {@code null})
     */
    public ServiceNode(String name, MethodHandle action, ServiceGraph graph, int dependentCount, ServiceNode dep) {
        this(name, action, graph, dependentCount, List.of(dep));
    }

    /**
     * Construct a new node with two dependencies.
     *
     * @param name the diagnostic name (must not be {@code null})
     * @param action the action handle (must not be {@code null})
     * @param graph the owning graph (must not be {@code null})
     * @param dependentCount the number of dependents this node will have
     * @param dep1 the first dependency (must not be {@code null})
     * @param dep2 the second dependency (must not be {@code null})
     */
    public ServiceNode(String name, MethodHandle action, ServiceGraph graph, int dependentCount,
            ServiceNode dep1, ServiceNode dep2) {
        this(name, action, graph, dependentCount, List.of(dep1, dep2));
    }

    /**
     * Construct a new node with an arbitrary number of dependencies.
     *
     * @param name the diagnostic name (must not be {@code null})
     * @param action the action handle (must not be {@code null})
     * @param graph the owning graph (must not be {@code null})
     * @param dependentCount the number of dependents this node will have
     * @param dependencies the dependency list (must not be {@code null}; defensively copied)
     */
    public ServiceNode(String name, MethodHandle action, ServiceGraph graph, int dependentCount,
            List<ServiceNode> dependencies) {
        this.name = name;
        this.action = action;
        this.graph = graph;
        this.dependencies = List.copyOf(dependencies);
        this.packed = pack(S_PENDING, dependencies.size(), dependentCount);
        for (ServiceNode dep : dependencies) {
            dep.addDependent(this);
        }
    }

    // ═══════════════════════════════════════════════
    // Construction-phase wiring
    // ═══════════════════════════════════════════════

    /**
     * Add a dependent to this node's mutable dependent list.
     * Must only be called during single-threaded graph construction.
     *
     * @param dependent the dependent node (must not be {@code null})
     */
    private void addDependent(ServiceNode dependent) {
        dependents.add(dependent);
    }

    // ═══════════════════════════════════════════════
    // Start path
    // ═══════════════════════════════════════════════

    /**
     * Run the node's action on the executor thread.
     * <p>
     * Transitions the node from {@code PENDING} to {@code RUNNING},
     * invokes the action handle, and relies on the action to signal
     * completion via {@link #startComplete(Object)} or
     * {@link #startComplete()}. If the action throws, the node
     * transitions to {@code FAILED} and the failure cascades.
     * <p>
     * If the node was canceled before the executor reached it (a
     * dependency failed between submission and execution), this
     * method returns immediately.
     */
    @Override
    public void run() {
        // transition PENDING → RUNNING
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (stateOf(old) != S_PENDING) {
                return; // canceled before execution
            }
            long next = pack(S_RUNNING, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        LOG.tracef("Running service '%s'", name);
        try {
            action.invokeExact(this);
        } catch (Throwable t) {
            // try to transition RUNNING → FAILED
            old = (long) PACKED.getVolatile(this);
            for (;;) {
                int s = stateOf(old);
                if (s != S_RUNNING) {
                    // action already signaled completion before throwing
                    if (s != S_COMPLETED && s != S_STOPPING && s != S_STOPPED) {
                        LOG.errorf(t, "Service '%s' threw after signaling %s", name, stateName(s));
                    }
                    return;
                }
                long next = pack(S_FAILED, remainingDepsOf(old), liveDependentsOf(old));
                long wit = (long) PACKED.compareAndExchange(this, old, next);
                if (wit == old) {
                    break;
                }
                old = wit;
            }
            propagateFailure(t);
        }
    }

    /**
     * Signal that the typed service action has completed successfully.
     * Stores the value and notifies dependents.
     * <p>
     * After transitioning to COMPLETED, checks whether all dependents
     * have already terminated (live-dependents == 0). If so, immediately
     * initiates the stop sequence — this handles the case where
     * {@link #dependentTerminated()} raced ahead of completion.
     *
     * @param value the service value (must not be {@code null})
     * @throws IllegalStateException if a completion method has already been called
     */
    @Override
    public void startComplete(Object value) {
        if (value == null) {
            throw new IllegalArgumentException("Service value must not be null; use a void service instead");
        }
        this.value = value;
        // transition RUNNING → COMPLETED
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (stateOf(old) != S_RUNNING) {
                throw new IllegalStateException(
                        "Service '" + name + "': completion method already called (state is " + stateName(stateOf(old)) + ")");
            }
            long next = pack(S_COMPLETED, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        notifyDependentsSatisfied();
        // only check for immediate stop if this node actually has dependents;
        // nodes with no dependents (e.g. the bottom sentinel) are stopped via
        // graph.stop() → initiateStop(), not by auto-stop on completion
        if (!dependents.isEmpty()) {
            tryInitiateStopIfReady();
        }
    }

    /**
     * Signal that the void service action has completed successfully.
     * Notifies dependents.
     * <p>
     * Idempotent for void services: subsequent calls are silently ignored.
     * After transitioning to COMPLETED, checks whether all dependents
     * have already terminated and initiates stop if so.
     */
    @Override
    public void startComplete() {
        // transition RUNNING → COMPLETED (idempotent: bail out if not RUNNING)
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (stateOf(old) != S_RUNNING) {
                return; // idempotent for void services
            }
            long next = pack(S_COMPLETED, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        notifyDependentsSatisfied();
        if (!dependents.isEmpty()) {
            tryInitiateStopIfReady();
        }
    }

    /**
     * Signal that the service start was canceled.
     * Dependents that consume this service non-optionally will fail to start.
     *
     * @throws IllegalStateException if {@link #startFailed(Throwable)} has already been called
     */
    @Override
    public void startCanceled() {
        // transition RUNNING → CANCELED
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            int s = stateOf(old);
            if (s == S_FAILED) {
                throw new IllegalStateException("Service '" + name + "': cannot cancel after failure");
            }
            if (s != S_RUNNING) {
                return; // already canceled, completed, or in another non-cancelable state
            }
            long next = pack(S_CANCELED, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        propagateCancel();
    }

    /**
     * Signal that the service start has failed.
     *
     * @param e the failure cause (must not be {@code null})
     * @throws IllegalStateException if a completion method has already been called
     */
    @Override
    public void startFailed(Throwable e) {
        // transition RUNNING → FAILED
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (stateOf(old) != S_RUNNING) {
                throw new IllegalStateException(
                        "Service '" + name + "': completion method already called (state is " + stateName(stateOf(old)) + ")");
            }
            long next = pack(S_FAILED, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        propagateFailure(e);
    }

    /**
     * Store the failure cause, log, and propagate to dependents (forward)
     * and dependencies (backward).
     * <p>
     * Must only be called after the state has already been set to
     * {@link #S_FAILED} via CAX.
     *
     * @param cause the failure cause
     */
    private void propagateFailure(Throwable cause) {
        this.failure = cause;
        graph.recordFailure(cause);
        LOG.errorf(cause, "Service '%s' failed to start", name);
        // forward: cancel dependents that haven't started yet
        for (ServiceNode dep : dependents) {
            dep.dependencyFailed();
        }
        // backward: notify our dependencies that we're terminal
        for (ServiceNode dep : dependencies) {
            dep.dependentTerminated();
        }
    }

    /**
     * Notify dependents that this dependency has been satisfied.
     */
    private void notifyDependentsSatisfied() {
        for (ServiceNode dep : dependents) {
            dep.dependencySatisfied();
        }
    }

    // ═══════════════════════════════════════════════
    // Dependency satisfaction
    // ═══════════════════════════════════════════════

    /**
     * Called by a dependency when it completes.
     * Atomically decrements the remaining dependency count; if it reaches
     * zero and the state is still {@code PENDING}, this node submits
     * itself to the executor.
     */
    void dependencySatisfied() {
        long old = (long) PACKED.getVolatile(this);
        long next;
        for (;;) {
            next = pack(stateOf(old), remainingDepsOf(old) - 1, liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        if (remainingDepsOf(next) == 0 && stateOf(next) == S_PENDING) {
            graph.executor().execute(this);
        }
    }

    // ═══════════════════════════════════════════════
    // Failure propagation (forward)
    // ═══════════════════════════════════════════════

    /**
     * Called when a dependency has failed or been canceled.
     * If this node is still pending, it transitions to canceled and
     * propagates the cancellation forward to its own dependents.
     */
    void dependencyFailed() {
        // transition PENDING → CANCELED
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (stateOf(old) != S_PENDING) {
                return; // already running, completed, failed, or canceled
            }
            long next = pack(S_CANCELED, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        // wake the main thread in case this is a sentinel node
        graph.signalMainThread();
        propagateCancel();
    }

    /**
     * Propagate cancellation: notify dependents of failure (forward)
     * and notify dependencies that this node is terminal (backward).
     */
    private void propagateCancel() {
        for (ServiceNode dep : dependents) {
            dep.dependencyFailed();
        }
        for (ServiceNode dep : dependencies) {
            dep.dependentTerminated();
        }
    }

    // ═══════════════════════════════════════════════
    // Stop path (backward propagation)
    // ═══════════════════════════════════════════════

    /**
     * Called when a dependent reaches a terminal state (stopped, failed,
     * or canceled). Atomically decrements the live-dependent count.
     * <p>
     * If the decrement brings the count to zero <em>and</em> the state is
     * {@code COMPLETED}, the transition to {@code STOPPING} happens in
     * the same CAX — this is the key advantage of the packed field, as it
     * eliminates the race where separate atomic fields could allow both
     * this method and {@link #startComplete()} to miss the combined
     * condition.
     */
    void dependentTerminated() {
        long old = (long) PACKED.getVolatile(this);
        boolean shouldStop = false;
        for (;;) {
            int ld = liveDependentsOf(old) - 1;
            int s = stateOf(old);
            long next;
            if (ld == 0 && s == S_COMPLETED) {
                // atomically decrement AND transition to STOPPING
                next = pack(S_STOPPING, remainingDepsOf(old), 0);
                shouldStop = true;
            } else {
                next = pack(s, remainingDepsOf(old), ld);
                shouldStop = false;
            }
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        if (shouldStop) {
            runStopHandler();
        }
    }

    /**
     * Initiate the stop sequence for this node.
     * Transitions from {@code COMPLETED} to {@code STOPPING} via CAX,
     * ensuring that only one thread can initiate the stop.
     * <p>
     * Called by {@link ServiceGraph#stop()} on the bottom sentinel
     * to begin the reverse cascade.
     */
    void initiateStop() {
        // transition COMPLETED → STOPPING
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (stateOf(old) != S_COMPLETED) {
                return;
            }
            long next = pack(S_STOPPING, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        runStopHandler();
    }

    /**
     * Check whether this node should stop immediately after completing.
     * <p>
     * If all dependents have already terminated (live-dependents == 0)
     * by the time the node reaches {@code COMPLETED}, this method
     * transitions to {@code STOPPING} and runs the stop handler.
     * Called by {@link #startComplete(Object)} and {@link #startComplete()}.
     */
    private void tryInitiateStopIfReady() {
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            if (liveDependentsOf(old) != 0 || stateOf(old) != S_COMPLETED) {
                return;
            }
            long next = pack(S_STOPPING, remainingDepsOf(old), 0);
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        runStopHandler();
    }

    /**
     * Run the registered stop handler (if any) and then complete the
     * stop sequence via {@link #finishStop()}.
     */
    private void runStopHandler() {
        Consumer<AsyncStopContext> handler = (Consumer<AsyncStopContext>) STOP_HANDLER.getVolatile(this);
        if (handler != null) {
            try {
                handler.accept(this::finishStop);
            } catch (Throwable t) {
                LOG.errorf(t, "Stop handler for service '%s' threw an exception", name);
                finishStop();
            }
        } else {
            finishStop();
        }
    }

    /**
     * Complete the stop sequence. Transitions to {@code STOPPED} and
     * notifies each dependency that this dependent has terminated,
     * potentially triggering their stop in turn.
     */
    private void finishStop() {
        // transition to STOPPED, preserving counter values
        long old = (long) PACKED.getVolatile(this);
        for (;;) {
            long next = pack(S_STOPPED, remainingDepsOf(old), liveDependentsOf(old));
            long wit = (long) PACKED.compareAndExchange(this, old, next);
            if (wit == old) {
                break;
            }
            old = wit;
        }
        // release the service value, stop handler, and dependent links so that
        // heavy objects (e.g. SSL contexts, thread pools) and the MethodHandle
        // action references (which pin the QuarkusClassLoader) are eligible for
        // GC as soon as the node graph is disconnected from its ServiceGraph
        this.value = null;
        STOP_HANDLER.setRelease(this, null);
        for (ServiceNode dep : dependencies) {
            dep.dependentTerminated();
        }
        dependents.clear();
    }

    // ═══════════════════════════════════════════════
    // Stop handler registration (StartContext interface)
    // ═══════════════════════════════════════════════

    /**
     * {@inheritDoc}
     * <p>
     * Only one stop handler may be registered per node. Subsequent calls
     * will throw {@link IllegalStateException}.
     */
    @Override
    public void onStopAsync(Consumer<AsyncStopContext> stopper) {
        if (!STOP_HANDLER.compareAndSet(this, null, stopper)) {
            throw new IllegalStateException("Service '" + name + "': stop handler already registered");
        }
    }

    // ═══════════════════════════════════════════════
    // Accessors
    // ═══════════════════════════════════════════════

    /**
     * Get the owning service graph.
     *
     * @return the graph (never {@code null})
     */
    public ServiceGraph graph() {
        return graph;
    }

    /**
     * Get the diagnostic name of this service node.
     *
     * @return the name (never {@code null})
     */
    public String name() {
        return name;
    }

    /**
     * Get the value produced by this service.
     * Only valid after the node has completed.
     *
     * @return the service value, or {@code null} for void services
     */
    public Object value() {
        return value;
    }

    /**
     * Get the value of a dependency by index.
     * The index corresponds to the position in the dependency list
     * passed to the constructor.
     *
     * @param index the dependency index
     * @return the dependency's value (may be {@code null} for void dependencies)
     */
    public Object dependencyValue(int index) {
        return dependencies.get(index).value;
    }

    /**
     * Get the current lifecycle state.
     *
     * @return the state constant
     */
    int state() {
        return stateOf((long) PACKED.getVolatile(this));
    }

    /**
     * Get the failure cause, if the node failed.
     *
     * @return the failure cause, or {@code null} if the node did not fail
     */
    public Throwable failure() {
        return failure;
    }

    /**
     * Get a human-readable name for a state constant.
     *
     * @param s the state constant
     * @return the state name
     */
    static String stateName(int s) {
        return switch (s) {
            case S_PENDING -> "PENDING";
            case S_RUNNING -> "RUNNING";
            case S_COMPLETED -> "COMPLETED";
            case S_FAILED -> "FAILED";
            case S_CANCELED -> "CANCELED";
            case S_STOPPING -> "STOPPING";
            case S_STOPPED -> "STOPPED";
            default -> "UNKNOWN(" + s + ")";
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "ServiceNode[" + name + ":" + stateName(state()) + "]";
    }
}
