package io.quarkus.qute.debug.server;

import io.quarkus.qute.debug.Breakpoint;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.DebuggerStoppedException;
import io.quarkus.qute.debug.StackFrame;
import io.quarkus.qute.debug.StackTrace;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.StoppedEvent.StoppedReason;
import io.quarkus.qute.debug.Thread;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.ThreadEvent.ThreadStatus;
import io.quarkus.qute.trace.ResolveEvent;
import java.util.LinkedList;
import java.util.function.Supplier;

public class RemoteThread {

    private DebuggerState state;

    private final Object lock;

    private final LinkedList<RemoteStackFrame> frames;

    private final Thread data;

    private final RemoteDebuggerServer server;

    private static final Supplier<Boolean> TRUE_CONDITION = () -> true;

    private Supplier<Boolean> stopCondition;

    public RemoteThread(java.lang.Thread thread, RemoteDebuggerServer server) {
        this.lock = new Object();
        this.frames = new LinkedList<>();
        this.data = new Thread(thread.getId(), thread.getName());
        this.server = server;
        this.state = DebuggerState.INITIALIZED;
    }

    public long getId() {
        return data.getId();
    }

    public String getName() {
        return data.getName();
    }

    public Thread getData() {
        return data;
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
                    throw new DebuggerStoppedException(null);
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
                    throw new DebuggerStoppedException(null);
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
            return; // throw new DebuggerStoppedException();
        }
        RemoteStackFrame frame = new RemoteStackFrame(event, getCurrentFrame());
        this.frames.addFirst(frame);
        String templateId = frame.getTemplateId();
        StackFrame previous = frame.getPrevious();

        if (this.stopCondition != null && this.stopCondition.get()) {
            // suspend and wait because of step reason.
            this.suspendAndWait(StoppedReason.STEP);
        } else {
            int lineNumber = frame.getLine();
            Breakpoint breakpoint = server.getBreakpoint(templateId, lineNumber);
            if (breakpoint != null && (previous == null || previous.getLine() != lineNumber)) {
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
                server.fireStoppedEvent(e);

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

    private StackFrame getCurrentFrame() {
        return this.frames.size() > 0 ? this.frames.getFirst().getData() : null;
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
        this.stopCondition = () -> {
            return this.frames.size() <= frameSize;
        };
        this.resume();
    }

    public StackTrace getStackTrace() {
        return new StackTrace(frames);
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
        this.server.fireThreadEvent(new ThreadEvent(getId(), ThreadStatus.STARTED));
    }

    public void exit() {
        this.server.fireThreadEvent(new ThreadEvent(getId(), ThreadStatus.EXITED));
    }

    public RemoteScope getScope(int variablesReference) {
        for (RemoteStackFrame frame : frames) {
            for (RemoteScope scope : frame.getScopes()) {
                if (variablesReference == scope.getVariablesReference()) {
                    return scope;
                }
            }
        }
        return null;
    }

}
