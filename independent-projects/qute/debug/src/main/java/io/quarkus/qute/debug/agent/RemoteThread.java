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

public class RemoteThread extends Thread {

    public static final Thread[] EMPTY_THREAD = new Thread[0];

    private static final Predicate<TemplateNode> TRUE_CONDITION = node -> true;

    private static final Predicate<TemplateNode> EXPRESSION_CONDITION = node -> {
        return !node.isText();
    };

    private transient DebuggerState state;

    private transient final Object lock;

    private transient final LinkedList<RemoteStackFrame> frames;

    private transient final DebuggeeAgent agent;

    private transient Predicate<TemplateNode> stopCondition;

    public RemoteThread(java.lang.Thread thread, DebuggeeAgent agent) {
        this.lock = new Object();
        this.frames = new LinkedList<>();
        super.setId((int) thread.getId());
        super.setName(thread.getName());
        this.agent = agent;
        this.state = DebuggerState.INITIALIZED;
    }

    public DebuggerState getState() {
        synchronized (this.lock) {
            return this.state;
        }
    }

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

    public boolean isStopped() {
        synchronized (this.lock) {
            return this.state == DebuggerState.STOPPED;
        }
    }

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
            if (breakpoint != null
                    && (previous == null || (!previous.getTemplateId().equals(templateId)) || previous.getLine() != lineNumber)
                    && breakpoint.checkCondition(frame)) {
                // suspend and wait because of breakpoint reason.
                this.suspendAndWait(StoppedReason.BREAKPOINT);
            }
        }
    }

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

    private RemoteStackFrame getCurrentFrame() {
        return !this.frames.isEmpty() ? this.frames.getFirst() : null;
    }

    public void terminate() {
        synchronized (this.lock) {
            this.state = DebuggerState.STOPPED;
            this.lock.notifyAll();
        }
    }

    public void stepIn() {
        this.stopCondition = TRUE_CONDITION;
        this.resume();
    }

    public void stepOut() {
        this.resume();
    }

    public void stepOver() {
        int frameSize = this.frames.size();
        this.stopCondition = node -> {
            return this.frames.size() <= frameSize;
        };
        this.resume();
    }

    public void next() {
        this.stopCondition = EXPRESSION_CONDITION;
        this.resume();
    }

    public List<RemoteStackFrame> getStackFrames() {
        return frames;
    }

    public RemoteStackFrame getStackFrame(int frameId) {
        for (RemoteStackFrame frame : frames) {
            if (frameId == frame.getId()) {
                return frame;
            }
        }
        return null;
    }

    public void start() {
        frames.clear();
        this.agent.fireThreadEvent(new ThreadEvent(getId(), ThreadStatus.STARTED));
    }

    public void exit() {
        this.agent.fireThreadEvent(new ThreadEvent(getId(), ThreadStatus.EXITED));
    }

}
