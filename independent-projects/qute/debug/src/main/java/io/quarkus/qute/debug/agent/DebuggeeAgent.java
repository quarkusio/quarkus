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
import org.eclipse.lsp4j.debug.StackTraceResponse;
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

/**
 * The {@code DebuggeeAgent} manages the debugging process for Qute templates.
 * <p>
 * It acts as the core component on the debuggee side:
 * <ul>
 * <li>Tracks and manages Qute {@link Engine} instances for debugging.</li>
 * <li>Manages breakpoints, stack frames, scopes, and variables.</li>
 * <li>Receives trace events from Qute templates and maps them to DAP
 * events.</li>
 * <li>Notifies registered {@link DebuggerListener}s of relevant events
 * (stopped, threads changes, output, termination, etc.).</li>
 * </ul>
 * </p>
 */
public class DebuggeeAgent implements Debugger {

    /**
     * The listener that tracks Qute execution and forwards events to this agent.
     */
    private final DebuggerTraceListener debugListener;

    /** Breakpoints mapped by template ID and line number. */
    private final Map<String, Map<Integer, RemoteBreakpoint>> breakpoints;

    /** Currently active debuggee threads mapped by thread ID. */
    private final Map<Integer, RemoteThread> debuggees;

    /** Registered listeners interested in debugging events. */
    private final Collection<DebuggerListener> listeners;

    /** Responsible for evaluating expressions within templates. */
    private final EvaluationSupport evaluationSupport;

    /** Responsible for providing completion suggestions within templates. */
    private final CompletionSupport completionSupport;

    /** Registry of variables used for scopes and evaluations. */
    private final VariablesRegistry variablesRegistry;

    /** Registry mapping Qute templates to DAP {@link Source} objects. */
    private final SourceTemplateRegistry sourceTemplateRegistry;

    /** The set of Qute engines being tracked for debugging. */
    private final Set<Engine> trackedEngine;

    /** Indicates whether the debugging agent is enabled. */
    private boolean enabled;

    /**
     * Creates a new {@link DebuggeeAgent} instance.
     */
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

    /**
     * Starts tracking the given Qute engine for debugging.
     *
     * @param engine the engine to track.
     */
    public void track(Engine engine) {
        if (!trackedEngine.contains(engine)) {
            engine.addTraceListener(debugListener);
            trackedEngine.add(engine);
        }
    }

    @Override
    public DebuggerState getState(int threadId) {
        RemoteThread thread = getRemoteThread(threadId);
        return thread != null ? thread.getState() : DebuggerState.UNKNOWN;
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

    /**
     * Called when a template starts executing.
     *
     * @param event the start template event.
     */
    public void onStartTemplate(TemplateEvent event) {
        if (!isEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebuggeeThread();
        debuggee.start();
    }

    /**
     * Called whenever a Qute template node is processed.
     *
     * @param event the resolve event representing the current node.
     */
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

    /**
     * Called when template execution ends.
     *
     * @param event the end template event.
     */
    public void onEndTemplate(TemplateEvent event) {
        if (!isEnabled()) {
            return;
        }
        RemoteThread debuggee = getOrCreateDebuggeeThread();
        debuggees.remove(debuggee.getId());
        debuggee.exit();
    }

    /**
     * Gets or creates a {@link RemoteThread} representing the current Java thread.
     *
     * @return the corresponding {@link RemoteThread}.
     */
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

    /**
     * Retrieves a debuggee thread by ID.
     */
    private RemoteThread getRemoteThread(int threadId) {
        return debuggees.get(threadId);
    }

    @Override
    public Breakpoint[] setBreakpoints(SourceBreakpoint[] sourceBreakpoints, Source source) {
        sourceTemplateRegistry.registerSource(source);
        String templateId = sourceTemplateRegistry.getTemplateId(source);
        Map<Integer, RemoteBreakpoint> templateBreakpoints = templateId != null
                ? this.breakpoints.computeIfAbsent(templateId, k -> new HashMap<>())
                : null;
        if (templateBreakpoints != null) {
            templateBreakpoints.clear();
        }

        Breakpoint[] result = new Breakpoint[sourceBreakpoints.length];
        for (int i = 0; i < sourceBreakpoints.length; i++) {
            SourceBreakpoint sourceBreakpoint = sourceBreakpoints[i];
            int line = sourceBreakpoint.getLine();
            String condition = sourceBreakpoint.getCondition();
            RemoteBreakpoint breakpoint = new RemoteBreakpoint(source, line, condition);
            if (templateBreakpoints != null) {
                templateBreakpoints.put(line, breakpoint);
                breakpoint.setVerified(true);
            } else {
                breakpoint.setVerified(false);
            }
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
        return debuggees.values().toArray(RemoteThread.EMPTY_THREAD);
    }

    /**
     * Retrieves a breakpoint for the given template and line number.
     *
     * @param templateId the template identifier.
     * @param line the line number.
     * @return the matching breakpoint, or {@code null} if none exists.
     */
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

    /**
     * Removes a debugger listener and unlocks all threads if there are no listeners
     * left.
     *
     * @param listener the listener to remove.
     */
    public void removeDebuggerListener(DebuggerListener listener) {
        listeners.remove(listener);
        if (listeners.isEmpty()) {
            unlockAllDebuggeeThreads();
        }
    }

    /**
     * Notifies all listeners of a stopped event.
     */
    void fireStoppedEvent(StoppedEvent event) {
        for (DebuggerListener listener : listeners) {
            try {
                listener.onStopped(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notifies all listeners of a thread change event.
     */
    void fireThreadEvent(ThreadEvent event) {
        for (DebuggerListener listener : listeners) {
            try {
                listener.onThreadChanged(event);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Notifies all listeners of a terminate event.
     */
    void fireTerminateEvent() {
        for (DebuggerListener listener : listeners) {
            try {
                listener.onTerminate();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Sends output to all listeners.
     */
    void output(OutputEventArguments args) {
        for (DebuggerListener listener : listeners) {
            try {
                listener.output(args);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Unlocks and terminates all debuggee threads, then clears all tracked engines.
     */
    public void unlockAllDebuggeeThreads() {
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
            unlockAllDebuggeeThreads();
            this.breakpoints.clear();
        } finally {
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
    public StackTraceResponse getStackFrames(int threadId, Integer startFrame, Integer levels) {
        StackTraceResponse response = new StackTraceResponse();
        RemoteThread thread = getRemoteThread(threadId);
        if (thread != null) {
            var frames = thread.getStackFrames();
            if (startFrame != null || levels != null) {
                int startIndex = startFrame != null ? startFrame : 0;
                int endIndex = Math.min(startIndex + (levels != null ? levels : frames.size()), frames.size());
                response.setStackFrames(
                        thread.getStackFrames().subList(startIndex, endIndex).toArray(EMPTY_STACK_FRAMES));
            } else {
                response.setStackFrames(frames.toArray(EMPTY_STACK_FRAMES));
            }
            response.setTotalFrames(frames.size());
        } else {
            response.setStackFrames(EMPTY_STACK_FRAMES);
            response.setTotalFrames(0);
        }
        return response;
    }

    @Override
    public Scope[] getScopes(int frameId) {
        for (RemoteThread thread : debuggees.values()) {
            RemoteStackFrame frame = thread.getStackFrame(frameId);
            if (frame != null) {
                return frame.getScopes().toArray(EMPTY_SCOPES);
            }
        }
        return EMPTY_SCOPES;
    }

    @Override
    public Variable[] getVariables(int variablesReference) {
        return variablesRegistry.getVariables(variablesReference);
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression, String context) {
        return evaluationSupport.evaluate(frameId, expression, context);
    }

    /**
     * Finds a stack frame by its ID across all threads.
     */
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

    /**
     * Resets the agent by unlocking all threads and clearing tracked engines.
     */
    public void reset() {
        unlockAllDebuggeeThreads();
    }
}
