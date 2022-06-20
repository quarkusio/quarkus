package io.quarkus.qute.debug.server;

import io.quarkus.qute.Engine;
import io.quarkus.qute.debug.Breakpoint;
import io.quarkus.qute.debug.Debugger;
import io.quarkus.qute.debug.DebuggerListener;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.Scope;
import io.quarkus.qute.debug.StackTrace;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.Variable;
import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class RemoteDebuggerServer extends UnicastRemoteObject implements Debugger {

    private static final long serialVersionUID = 1872583681787561127L;

    private final DebuggerTraceListener debugListener;

    private final Map<String, Map<Integer, Breakpoint>> breakpoints;

    private final Map<Long, RemoteThread> debuggees;

    private final Collection<DebuggerListener> listeners;

    private RemoteDebuggerServer() throws RemoteException {
        super();
        this.debugListener = new DebuggerTraceListener(this);
        this.breakpoints = new HashMap<>();
        this.debuggees = new HashMap<>();
        this.listeners = new ArrayList<>();
    }

    public void track(Engine engine) {
        engine.addTraceListener(debugListener);
    }

    public static RemoteDebuggerServer createDebugger() throws RemoteException {
        return createDebugger(Debugger.DEFAULT_PORT);
    }

    public static RemoteDebuggerServer createDebugger(int port) throws RemoteException {
        Registry registry = LocateRegistry.createRegistry(port);
        RemoteDebuggerServer server = new RemoteDebuggerServer();
        registry.rebind(Debugger.DEBUGGER_NAME, server);
        return server;
    }

    @Override
    public DebuggerState getState(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        return thread != null ? thread.getState() : DebuggerState.UNKWOWN;
    }

    @Override
    public void pause(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.pause();
        }
    }

    @Override
    public void resume(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.resume();
        }
    }

    public void onStartTemplate(TemplateEvent event) throws RemoteException {
        if (!isTrackEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebugeeThread();
        debuggee.start();
    }

    public void onTemplateNode(ResolveEvent event) throws RemoteException {
        if (!isTrackEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebugeeThread();
        debuggee.onTemplateNode(event);
    }

    public void onEndTemplate(TemplateEvent event) throws RemoteException {
        if (!isTrackEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebugeeThread();
        debuggees.remove(debuggee.getId());
        debuggee.exit();
    }

    private boolean isTrackEnabled() {
        // Tracking template nodes is enabled only if
        // - client debugger has registered some debugger listeners
        // - client debugger has registered some breakpoints
        return !listeners.isEmpty() || !breakpoints.isEmpty();
    }

    private RemoteThread getOrCreateDebugeeThread() {
        Thread thread = Thread.currentThread();
        long threadId = thread.getId();
        RemoteThread debuggee = getRemoteThread(threadId);
        if (debuggee == null) {
            debuggee = new RemoteThread(thread, this);
            debuggees.put(threadId, debuggee);
        }
        return debuggee;
    }

    private RemoteThread getRemoteThread(long threadId) {
        return debuggees.get(threadId);
    }

    @Override
    public synchronized Breakpoint setBreakpoint(String templateId, int line) {
        Breakpoint breakpoint = new Breakpoint(templateId, line);
        Map<Integer, Breakpoint> templateBreakpoints = this.breakpoints.get(templateId);
        if (templateBreakpoints == null) {
            templateBreakpoints = new HashMap<>();
            this.breakpoints.put(templateId, templateBreakpoints);
        }
        templateBreakpoints.put(line, breakpoint);
        return breakpoint;
    }

    @Override
    public io.quarkus.qute.debug.Thread getThread(long threadId) {
        RemoteThread thread = debuggees.get(threadId);
        if (thread != null) {
            return thread.getData();
        }
        return null;
    }

    @Override
    public io.quarkus.qute.debug.Thread[] getThreads() {
        return debuggees.values() //
                .stream() //
                .map(t -> t.getData()) //
                .collect(Collectors.toList()) //
                .toArray(new io.quarkus.qute.debug.Thread[0]);
    }

    @Override
    public Breakpoint getBreakpoint(String templateId, int line) {
        Map<Integer, Breakpoint> templateBreakpoints = this.breakpoints.get(templateId);
        return templateBreakpoints != null ? templateBreakpoints.get(line) : null;
    }

    @Override
    public void addDebuggerListener(DebuggerListener listener) {
        listeners.add(listener);
    }

    public void removeDebuggerListener(DebuggerListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            // The remote client debugger is disconnected, unlock all debuggee threads.
            unlockAllDebugeeThreads();
        }
    }

    void fireStoppedEvent(StoppedEvent event) {
        for (DebuggerListener listener : listeners) {
            try {
                listener.onStopped(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void fireThreadEvent(ThreadEvent event) {
        for (DebuggerListener listener : listeners) {
            try {
                listener.onThreadChanged(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void fireTerminateEvent() {
        for (DebuggerListener listener : listeners) {
            try {
                listener.onTerminate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void unlockAllDebugeeThreads() {
        try {
            // Terminate all current debugee Thread.
            for (RemoteThread thread : debuggees.values()) {
                thread.terminate();
            }
            debuggees.clear();
            // Remove all breakpoints
            this.breakpoints.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void terminate() {
        try {
            // Terminate all current debugee Thread.
            unlockAllDebugeeThreads();
        } finally {
            // Notify that debugger server is terminated.
            fireTerminateEvent();
        }
    }

    @Override
    public void stepIn(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.stepIn();
        }
    }

    @Override
    public void stepOut(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.stepOut();
        }
    }

    @Override
    public void stepOver(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.stepOver();
        }
    }

    @Override
    public StackTrace getStackTrace(long threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            return thread.getStackTrace();
        }
        return null;
    }

    @Override
    public Scope[] getScopes(int frameId) {
        for (RemoteThread thread : debuggees.values()) {
            RemoteStackFrame frame = thread.getStackFrame(frameId);
            if (frame != null) {
                return frame.getScopes() //
                        .stream() //
                        .map(s -> s.getData()) //
                        .collect(Collectors.toList()) //
                        .toArray(new Scope[0]);
            }
        }
        return new Scope[0];
    }

    @Override
    public Variable[] getVariables(int variablesReference) {
        for (RemoteThread thread : debuggees.values()) {
            RemoteScope scope = thread.getScope(variablesReference);
            if (scope != null) {
                return scope.getVariables() //
                        .toArray(new Variable[0]);
            }
        }
        return new Variable[0];
    }

}
