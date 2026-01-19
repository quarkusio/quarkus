package io.quarkus.qute.debug.agent;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;

import org.eclipse.lsp4j.debug.Thread;

import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.DebuggerStoppedException;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.StoppedEvent.StoppedReason;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.ThreadEvent.ThreadStatus;
import io.quarkus.qute.debug.agent.breakpoints.RemoteBreakpoint;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.frames.SectionFrameGroup;
import io.quarkus.qute.trace.ResolveEvent;

/**
 * Represents a Qute debug thread that manages the execution flow of templates during debugging.
 * <p>
 * This class extends the Debug Adapter Protocol {@link Thread} to track the current state
 * of execution, stack frames, and debugging actions like step over, step in, breakpoints, etc.
 * </p>
 *
 * <p>
 * It is tightly integrated with {@link DebuggeeAgent}, which communicates with
 * the debugging client.
 * </p>
 */
public class RemoteThread extends Thread {

    /**
     * Represents an empty array of threads.
     */
    public static final Thread[] EMPTY_THREAD = new Thread[0];

    /**
     * Default condition used to stop execution immediately.
     */
    private static final Predicate<TemplateNode> TRUE_CONDITION = node -> true;

    /**
     * Condition used to stop execution only on non-text template nodes.
     */
    private static final Predicate<TemplateNode> EXPRESSION_CONDITION = node -> {
        return !node.isText();
    };

    /**
     * Current state of the thread (e.g., running, suspended, stopped).
     */
    private transient DebuggerState state;

    /**
     * Lock object for synchronization of thread state changes.
     */
    private transient final Object lock;

    /**
     * Linked list of stack frames representing the current execution stack.
     */
    private transient final LinkedList<RemoteStackFrame> frames;

    private transient final LinkedList<SectionFrameGroup> sectionFrameStack;

    /**
     * Reference to the agent managing this thread.
     */
    private transient final DebuggeeAgent agent;

    /**
     * Condition that determines when the thread should stop execution.
     */
    private transient Predicate<TemplateNode> stopCondition;

    // Pending task to be executed during suspension
    private transient Callable<CompletableFuture<Object>> pendingTask;
    private transient CompletableFuture<Object> taskResult;

    /**
     * Creates a new {@link RemoteThread} instance.
     *
     * @param thread the actual Java thread being debugged
     * @param agent the associated {@link DebuggeeAgent}
     */
    public RemoteThread(java.lang.Thread thread, DebuggeeAgent agent) {
        this.lock = new Object();
        this.frames = new LinkedList<>();
        this.sectionFrameStack = new LinkedList<>();
        super.setId((int) thread.getId());
        super.setName(thread.getName());
        this.agent = agent;
        this.state = DebuggerState.INITIALIZED;
    }

    /**
     * Returns the current debugger state of this thread.
     *
     * @return the current {@link DebuggerState}
     */
    public DebuggerState getState() {
        synchronized (this.lock) {
            return this.state;
        }
    }

    /**
     * Pauses the thread at the next opportunity by setting the stop condition to always true.
     *
     * @throws DebuggerStoppedException if the thread is already stopped
     */
    public void pause() {
        synchronized (this.lock) {
            switch (state) {
                case STOPPED -> throw new DebuggerStoppedException();
                case RUNNING -> this.stopCondition = TRUE_CONDITION;
                default -> throw new IllegalStateException();
            }
        }
    }

    /**
     * Resumes execution of the thread after being suspended.
     *
     * @throws DebuggerStoppedException if the thread is already stopped
     * @throws IllegalStateException if the thread is not currently suspended
     */
    public void resume() {
        synchronized (this.lock) {
            switch (state) {
                case STOPPED -> throw new DebuggerStoppedException();
                default -> {
                    if (state != DebuggerState.SUSPENDED) {
                        throw new IllegalStateException();
                    }
                    this.state = DebuggerState.RUNNING;
                    this.lock.notifyAll();
                }
            }
        }
    }

    /**
     * Checks if the thread has been stopped.
     *
     * @return {@code true} if stopped, {@code false} otherwise
     */
    public boolean isStopped() {
        synchronized (this.lock) {
            return this.state == DebuggerState.STOPPED;
        }
    }

    /**
     * Called before a Qute template node is resolved.
     * <p>
     * Updates the current stack frames and handles section frame groups for
     * template sections (loops, conditionals, etc.).
     * </p>
     *
     * <p>
     * Sections include constructs like #for, #each, #if, and any other Qute
     * section nodes. Each section has its own SectionFrameGroup which tracks
     * the stack frames created while rendering that section.
     * </p>
     *
     * @param event the ResolveEvent representing the node resolution
     */
    public void onBeforeResolve(ResolveEvent event) {
        if (this.isStopped()) {
            return;
        }

        var engine = event.getEngine();

        // Create a new stack frame for the current node
        RemoteStackFrame frame = new RemoteStackFrame(
                event,
                getCurrentFrame(),
                agent.getSourceTemplateRegistry(engine),
                agent.getVariablesRegistry(),
                this);

        // Handle section frames (loops / #for / #each / #if / other sections)
        var sectionGroup = sectionFrameStack.isEmpty() ? null : sectionFrameStack.getFirst();
        if (sectionGroup != null) {
            // Update the section frame group index if iteration has changed and detach old frames
            sectionGroup.detachFramesIfIndexChanged(event.getContext().getData(), frames);
            // Add the current frame to the section group
            sectionGroup.addFrame(frame);
        }

        // If the current node is a section, push a new SectionFrameGroup to track its frames
        if (event.getTemplateNode().isSection()) {
            sectionFrameStack.addFirst(new SectionFrameGroup(event.getTemplateNode().asSection().getHelper()));
        }

        // Add the frame to the main thread stack
        this.frames.addFirst(frame);

        String templateId = frame.getTemplateId();
        URI sourceUri = frame.getTemplateUri();
        RemoteStackFrame previous = frame.getPrevious();

        // Step handling: suspend if stop condition matches
        if (this.stopCondition != null && this.stopCondition.test(event.getTemplateNode())) {
            // suspend and wait because of step reason.
            this.suspendAndWait(StoppedReason.STEP);
        } else {
            // Breakpoint handling: suspend if a breakpoint matches this frame
            int lineNumber = frame.getLine();
            RemoteBreakpoint breakpoint = agent.getBreakpoint(sourceUri, templateId, lineNumber, engine);
            if (breakpoint != null // a breakpoint matches the frame line number
                    && (previous == null
                            || !previous.getTemplateId().equals(templateId)
                            || previous.getLine() != lineNumber
                            || event.getTemplateNode().isExpression())
                    && breakpoint.checkCondition(frame)) {
                // suspend and wait because of breakpoint reason.
                this.suspendAndWait(StoppedReason.BREAKPOINT);
            }
        }
    }

    /**
     * Called after a Qute template node has been resolved.
     * <p>
     * Cleans up section frame groups if the resolved node was a section.
     * This includes loops (#for / #each), conditionals (#if), or any other
     * section nodes. Frames accumulated during the section are detached from
     * the main thread stack when the section ends.
     * </p>
     *
     * @param event the ResolveEvent representing the node resolution
     */
    public void onAfterResolve(ResolveEvent event) {
        if (this.isStopped()) {
            return;
        }

        // If the node is a section, pop its SectionFrameGroup and detach its frames
        if (event.getTemplateNode().isSection()) {
            var sectionGroup = sectionFrameStack.removeFirst();
            sectionGroup.detachFrames(frames);
        }
    }

    /**
     * Suspends execution of the render thread and waits for debugger interaction,
     * such as "step", "resume", or expression evaluation.
     * <p>
     * When the render thread reaches a breakpoint or step condition, it enters this
     * method and waits until the debugger instructs it to resume. While suspended,
     * the debugger may schedule a task (see {@link #evaluateInRenderThread(Callable)})
     * to be executed <b>within the render thread context</b>.
     * </p>
     *
     * <p>
     * This is essential for Qute expression evaluation — for example, evaluating
     * {@code uri:Todos.index} requires the active HTTP request context
     * ({@code @RequestScoped} beans). Evaluating outside this thread (e.g. from
     * the debugger thread) would cause a
     * {@code javax.enterprise.context.ContextNotActiveException: RequestScoped was not active}.
     * </p>
     *
     * @param reason the reason for suspension (e.g. breakpoint, step)
     */
    private void suspendAndWait(StoppedReason reason) {
        boolean intr = false;
        try {
            synchronized (this.lock) {
                // Mark thread as suspended and notify the debugger
                this.state = DebuggerState.SUSPENDED;
                this.lock.notifyAll();
                this.stopCondition = null;

                // Notify the debugger client that the thread has stopped
                StoppedEvent e = new StoppedEvent(getId(), reason);
                agent.fireStoppedEvent(e);

                // Loop until the debugger resumes or stops the thread
                while (this.state == DebuggerState.SUSPENDED) {

                    try {
                        // If a pending task (e.g., expression evaluation) was scheduled by the debugger:
                        if (pendingTask != null) {
                            try {
                                // Execute the task directly in this render thread context.
                                // This ensures access to the active HTTP request context,
                                // avoiding "RequestScoped was not active" errors.
                                taskResult = pendingTask.call();
                            } catch (Exception e1) {
                                // Should never happen — failures are handled by the caller
                            } finally {
                                // Clear the pending task and notify the waiting debugger
                                pendingTask = null;
                                lock.notifyAll();
                            }
                        }

                        // Continue waiting until resume/step or another evaluation task is scheduled
                        this.lock.wait(50); // small timeout to periodically recheck
                    } catch (InterruptedException ignored) {
                        intr = true;
                    }
                }

                // If resumed or stopped, the method exits, letting template rendering continue
            }
        } finally {
            if (intr) {
                java.lang.Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Schedules a task to be executed within the render thread (the thread currently rendering the template)
     * while it is suspended during debugging.
     * <p>
     * This method is typically used by the debugger to evaluate Qute expressions or inspect the current
     * template state. Since certain Qute features — such as expressions like {@code uri:Todos.index} —
     * rely on contextual data bound to the HTTP request thread (e.g. {@code @RequestScoped} beans),
     * these evaluations must occur in the same thread that is rendering the template.
     * Executing such expressions in any other thread would lead to context-related errors such as:
     *
     * <pre>
     *   javax.enterprise.context.ContextNotActiveException: RequestScoped was not active
     * </pre>
     * </p>
     *
     * <p>
     * The render thread periodically checks for scheduled evaluation tasks while suspended (see
     * {@link #suspendAndWait(io.quarkus.qute.debug.StoppedEvent.StoppedReason)}). When a task is detected,
     * it is executed directly within that render thread to ensure full access to the active request
     * and template context.
     * </p>
     *
     * <p>
     * <b>Threading model:</b><br>
     * This method must be called from the debugger control thread (not the render thread itself).
     * The provided callable will be executed synchronously within the render thread context, ensuring
     * compatibility with request-bound state.
     * </p>
     *
     * @param action the task to execute within the render thread context.
     *        It should return a {@link CompletableFuture} representing the computation result.
     * @return a {@link CompletableFuture} containing the result of the executed task
     * @throws IllegalStateException if the thread is not currently suspended or if another
     *         evaluation task is already pending
     */
    public CompletableFuture<Object> evaluateInRenderThread(Callable<CompletableFuture<Object>> action) {
        synchronized (lock) {
            // Ensure the render thread is suspended before executing any task
            if (this.state != DebuggerState.SUSPENDED) {
                throw new IllegalStateException("Thread not suspended");
            }

            // Prevent concurrent evaluation tasks from overlapping
            if (pendingTask != null) {
                throw new IllegalStateException("A task is already pending");
            }

            // Schedule the evaluation task to be executed by the render thread
            pendingTask = action;
            taskResult = null;

            // Wake up the suspended render thread so it can pick up and execute the task
            lock.notifyAll();

            // Wait for the render thread to process and complete the pending task
            boolean intr = false;
            try {
                while (pendingTask != null) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            } finally {
                if (intr) {
                    java.lang.Thread.currentThread().interrupt();
                }
            }

            // Return the computed result back to the debugger control thread
            return taskResult;
        }
    }

    /**
     * Returns the current active stack frame, or {@code null} if none exists.
     *
     * @return the current {@link RemoteStackFrame} or {@code null}
     */
    private RemoteStackFrame getCurrentFrame() {
        return !this.frames.isEmpty() ? this.frames.getFirst() : null;
    }

    /**
     * Terminates the thread and notifies any waiting processes.
     */
    public void terminate() {
        synchronized (this.lock) {
            this.state = DebuggerState.STOPPED;
            this.lock.notifyAll();
        }
    }

    /**
     * Performs a "step in" operation, stopping execution at the next node.
     */
    public void stepIn() {
        this.stopCondition = TRUE_CONDITION;
        this.resume();
    }

    /**
     * Performs a "step out" operation, resuming execution until the current
     * function or template block returns.
     */
    public void stepOut() {
        this.resume();
    }

    /**
     * Performs a "step over" operation, stopping only when the current frame size decreases,
     * i.e., when the current step is completed.
     */
    public void stepOver() {
        int frameSize = this.frames.size();
        this.stopCondition = node -> this.frames.size() <= frameSize;
        this.resume();
    }

    /**
     * Performs a "next" operation, stopping only on non-text nodes.
     */
    public void next() {
        this.stopCondition = EXPRESSION_CONDITION;
        this.resume();
    }

    /**
     * Returns the list of current stack frames.
     *
     * @return a list of {@link RemoteStackFrame}
     */
    public List<RemoteStackFrame> getStackFrames() {
        return frames;
    }

    /**
     * Finds a specific stack frame by its ID.
     *
     * @param frameId the frame ID
     * @return the corresponding {@link RemoteStackFrame}, or {@code null} if not found
     */
    public RemoteStackFrame getStackFrame(int frameId) {
        for (RemoteStackFrame frame : frames) {
            if (frameId == frame.getId()) {
                return frame;
            }
        }
        return null;
    }

    /**
     * Marks the thread as started and clears its stack frames.
     */
    public void start() {
        frames.clear();
        this.agent.fireThreadEvent(new ThreadEvent(getId(), ThreadStatus.STARTED));
    }

    /**
     * Marks the thread as exited and notifies the agent.
     */
    public void exit() {
        this.agent.fireThreadEvent(new ThreadEvent(getId(), ThreadStatus.EXITED));
    }

}
