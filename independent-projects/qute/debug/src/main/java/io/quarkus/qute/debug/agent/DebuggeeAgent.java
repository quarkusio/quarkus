package io.quarkus.qute.debug.agent;

import static io.quarkus.qute.debug.agent.RemoteStackFrame.EMPTY_STACK_FRAMES;
import static io.quarkus.qute.debug.agent.scopes.RemoteScope.EMPTY_SCOPES;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.OutputEventArgumentsCategory;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.Variable;

import io.quarkus.qute.Engine;
import io.quarkus.qute.debug.Debugger;
import io.quarkus.qute.debug.DebuggerListener;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.ThreadEvent.ThreadStatus;
import io.quarkus.qute.debug.agent.completions.CompletionSupport;
import io.quarkus.qute.debug.agent.evaluations.EvaluationSupport;
import io.quarkus.qute.debug.agent.variables.VariablesRegistry;
import io.quarkus.qute.trace.ResolveEvent;
import io.quarkus.qute.trace.TemplateEvent;

public class DebuggeeAgent implements Debugger {

    private final DebuggerTraceListener debugListener;

    private final Map<String /* template id */, Map<Integer, RemoteBreakpoint>> breakpoints;

    private final Map<Integer /* Thread id */, RemoteThread> debuggees;

    private final Collection<DebuggerListener> listeners;

    private final EvaluationSupport evaluationSupport;
    private final CompletionSupport completionSupport;

    private final VariablesRegistry variablesRegistry;
    private final SourceTemplateRegistry sourceTemplateRegistry;
    private final Set<Engine> trackedEngine;
    private boolean enabled;

    public DebuggeeAgent() {
        this.debugListener = new DebuggerTraceListener(this);
        this.breakpoints = new ConcurrentHashMap<>();
        this.debuggees = new ConcurrentHashMap<>();
        this.listeners = new ArrayList<>();
        this.variablesRegistry = new VariablesRegistry();
        this.sourceTemplateRegistry = new SourceTemplateRegistry();
        this.trackedEngine = new HashSet<>();
        this.evaluationSupport = new EvaluationSupport(this);
        this.completionSupport = new CompletionSupport(this);
    }

    public void track(Engine engine) {
        if (!trackedEngine.contains(engine)) {
            engine.addTraceListener(debugListener);
            trackedEngine.add(engine);
        }
    }

    @Override
    public DebuggerState getState(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        return thread != null ? thread.getState() : DebuggerState.UNKWOWN;
    }

    @Override
    public void pause(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.pause();
        }
    }

    @Override
    public void resume(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.resume();
        }
    }

    public void onStartTemplate(TemplateEvent event) {
        if (!isEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebuggeeThread();
        debuggee.start();
    }

    public void onTemplateNode(ResolveEvent event) {
        if (!isEnabled()) {
            return;
        }

        OutputEventArguments args = new OutputEventArguments();
        args.setOutput(event.getTemplateNode().toString());
        args.setCategory(OutputEventArgumentsCategory.CONSOLE);
        output(args);

        RemoteThread debuggee = getOrCreateDebuggeeThread();
        debuggee.onTemplateNode(event);
    }

    public void onEndTemplate(TemplateEvent event) {
        if (!isEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebuggeeThread();
        debuggees.remove(debuggee.getId());
        debuggee.exit();
    }

    private RemoteThread getOrCreateDebuggeeThread() {
        java.lang.Thread thread = java.lang.Thread.currentThread();
        int threadId = (int) thread.getId();
        RemoteThread debuggee = getRemoteThread(threadId);
        if (debuggee == null) {
            debuggee = new RemoteThread(thread, this);
            debuggees.put(threadId, debuggee);
        }
        return debuggee;
    }

    private RemoteThread getRemoteThread(int threadId) {
        return debuggees.get(threadId);
    }

    @Override
    public Breakpoint[] setBreakpoints(SourceBreakpoint[] sourceBreakpoints, Source source) {
        sourceTemplateRegistry.registerSource(source);
        String templateId = sourceTemplateRegistry.getTemplateId(source);
        Map<Integer, RemoteBreakpoint> templateBreakpoints = this.breakpoints.computeIfAbsent(templateId,
                k -> new HashMap<>());
        templateBreakpoints.clear();

        Breakpoint[] result = new Breakpoint[sourceBreakpoints.length];
        for (int i = 0; i < sourceBreakpoints.length; i++) {
            SourceBreakpoint sourceBreakpoint = sourceBreakpoints[i];
            int line = sourceBreakpoint.getLine();
            String condition = sourceBreakpoint.getCondition();
            RemoteBreakpoint breakpoint = new RemoteBreakpoint(source, line, condition);
            templateBreakpoints.put(line, breakpoint);

            breakpoint.setVerified(true);
            result[i] = breakpoint;
        }
        return result;
    }

    @Override
    public Thread getThread(int threadId) {
        return debuggees.get(threadId);
    }

    @Override
    public Thread[] getThreads() {
        return debuggees.values() //
                .toArray(RemoteThread.EMPTY_THREAD);
    }

    RemoteBreakpoint getBreakpoint(String templateId, int line) {
        Map<Integer, RemoteBreakpoint> templateBreakpoints = this.breakpoints.get(templateId);
        if (templateBreakpoints == null) {
            for (var fileExtension : sourceTemplateRegistry.getFileExtensions()) {
                templateBreakpoints = this.breakpoints.get(templateId + fileExtension);
                if (templateBreakpoints != null) {
                    break;
                }
            }
        }
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
            unlockAllDebuggeeThreads();
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

    void output(OutputEventArguments args) {
        for (DebuggerListener listener : listeners) {
            try {
                listener.output(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void unlockAllDebuggeeThreads() {
        // Terminate all current debuggee Thread.
        for (RemoteThread thread : debuggees.values()) {
            thread.terminate();
            fireThreadEvent(new ThreadEvent(thread.getId(), ThreadStatus.EXITED));
        }
        debuggees.clear();
        trackedEngine.forEach(engine -> engine.removeTraceListener(debugListener));
        trackedEngine.clear();
    }

    @Override
    public void terminate() {
        try {
            // Terminate all current debuggee Thread.
            unlockAllDebuggeeThreads();
            // Remove all breakpoints
            this.breakpoints.clear();
        } finally {
            // Notify that debugger server is terminated.
            fireTerminateEvent();
        }
    }

    @Override
    public void stepIn(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.stepIn();
        }
    }

    @Override
    public void stepOut(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.stepOut();
        }
    }

    @Override
    public void stepOver(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.stepOver();
        }
    }

    @Override
    public void next(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            thread.next();
        }
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return completionSupport.completions(args);
    }

    @Override
    public StackFrame[] getStackFrames(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            return thread.getStackFrames()//
                    .toArray(EMPTY_STACK_FRAMES);
        }
        return null;
    }

    @Override
    public Scope[] getScopes(int frameId) {
        for (RemoteThread thread : debuggees.values()) {
            RemoteStackFrame frame = thread.getStackFrame(frameId);
            if (frame != null) {
                return frame.getScopes() //
                        .toArray(EMPTY_SCOPES);
            }
        }
        return EMPTY_SCOPES;
    }

    @Override
    public Variable[] getVariables(int variablesReference) {
        return variablesRegistry.getVariables(variablesReference);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression) {
        return evaluationSupport.evaluate(frameId, expression);
    }

    public RemoteStackFrame findStackFrame(Integer frameId) {
        if (frameId == null) {
            return null;
        }
        for (RemoteThread thread : debuggees.values()) {
            RemoteStackFrame frame = thread.getStackFrame(frameId);
            if (frame != null) {
                return frame;
            }
        }
        return null;
    }

    public VariablesRegistry getVariablesRegistry() {
        return variablesRegistry;
    }

    public SourceTemplateRegistry getSourceTemplateRegistry() {
        return sourceTemplateRegistry;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void reset() {
        unlockAllDebuggeeThreads();
    }
}
