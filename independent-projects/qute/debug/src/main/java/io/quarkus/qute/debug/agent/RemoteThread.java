package io.quarkus.qute.debug.agent;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.eclipse.lsp4j.debug.Thread;

import io.quarkus.qute.TemplateNode;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.DebuggerStoppedException;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.StoppedEvent.StoppedReason;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.ThreadEvent.ThreadStatus;
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

    /**
     * Reference to the agent managing this thread.
     */
    private transient final DebuggeeAgent agent;

    /**
     * Condition that determines when the thread should stop execution.
     */
    private transient Predicate<TemplateNode> stopCondition;

    /**
     * Creates a new {@link RemoteThread} instance.
     *
     * @param thread the actual Java thread being debugged
     * @param agent the associated {@link DebuggeeAgent}
     */
    public RemoteThread(java.lang.Thread thread, DebuggeeAgent agent) {
        this.lock = new Object();
        this.frames = new LinkedList<>();
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
                case STOPPED:
                    throw new DebuggerStoppedException();
                case RUNNING:
                    this.stopCondition = TRUE_CONDITION;
                    break;
                default:
                    throw new IllegalStateException();
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
                case STOPPED:
                    throw new DebuggerStoppedException();
                case RUNNING:
                default:
                    if (state != DebuggerState.SUSPENDED) {
                        throw new IllegalStateException();
                    }
                    this.state = DebuggerState.RUNNING;
                    this.lock.notifyAll();
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
     * Called when a Qute template node is about to be resolved.
     * <p>
     * This method updates the stack frames and determines whether
     * execution should be suspended due to a breakpoint or step condition.
     * </p>
     *
     * @param event the {@link ResolveEvent} representing the node resolution
     */
    public void onTemplateNode(ResolveEvent event) {
        if (this.isStopped()) {
            return;
        }

        RemoteStackFrame frame = new RemoteStackFrame(event, getCurrentFrame(), agent.getSourceTemplateRegistry(),
                agent.getVariablesRegistry());
        this.frames.addFirst(frame);
        String templateId = frame.getTemplateId();
        RemoteStackFrame previous = frame.getPrevious();

        if (this.stopCondition != null && this.stopCondition.test(event.getTemplateNode())) {
            // suspend and wait because of step reason.
            this.suspendAndWait(StoppedReason.STEP);
        } else {
            int lineNumber = frame.getLine();
            RemoteBreakpoint breakpoint = agent.getBreakpoint(templateId, lineNumber);
            if (breakpoint != null // a breakpoint matches the frame line number
                    && (previous == null || !previous.getTemplateId().equals(templateId) || previous.getLine() != lineNumber
                            || event.getTemplateNode().isExpression())
                    && breakpoint.checkCondition(frame)) {
                // suspend and wait because of breakpoint reason.
                this.suspendAndWait(StoppedReason.BREAKPOINT);
            }
        }
    }

    /**
     * Suspends execution and waits for user interaction (e.g., step or resume).
     *
     * @param reason the reason for stopping (step, breakpoint, etc.)
     */
    private void suspendAndWait(StoppedReason reason) {
        try {
            synchronized (this.lock) {
                this.state = DebuggerState.SUSPENDED;
                this.lock.notifyAll();
                this.stopCondition = null;

                StoppedEvent e = new StoppedEvent(getId(), reason);
                agent.fireStoppedEvent(e);

                while (this.state == DebuggerState.SUSPENDED) {
                    this.lock.wait();
                }

                if (this.state == DebuggerState.STOPPED) {
                    // throw new DebuggerStoppedException();
                }
            }
        } catch (InterruptedException e) {
            // throw new DebuggerStoppedException();
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
        this.stopCondition = node -> {
            return this.frames.size() <= frameSize;
        };
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
