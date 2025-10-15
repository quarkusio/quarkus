package io.quarkus.qute.debug;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.SourceResponse;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.Variable;

/**
 * Qute debugger API.
 */
public interface Debugger {

    /**
     * Returns the current state of the remote debugger for a given thread.
     *
     * @param threadId the ID of the thread
     * @return the current {@link DebuggerState} of the thread
     */
    DebuggerState getState(int threadId);

    /**
     * Pauses the execution of the template engine on the specified thread.
     *
     * @param threadId the ID of the thread to pause
     */
    void pause(int threadId);

    /**
     * Resumes the execution of the template engine on the specified thread.
     *
     * @param threadId the ID of the thread to resume
     */
    void resume(int threadId);

    /**
     * Sets breakpoints in a source file.
     *
     * @param sourceBreakpoints the breakpoints to set
     * @param source the source file where breakpoints are applied
     * @return the actual breakpoints that were set
     */
    Breakpoint[] setBreakpoints(SourceBreakpoint[] sourceBreakpoints, Source source);

    /**
     * Returns the list of threads known to the debugger.
     *
     * @return an array of {@link Thread} objects
     */
    Thread[] getThreads();

    /**
     * Returns the thread with the given ID.
     *
     * @param threadId the ID of the thread
     * @return the corresponding {@link Thread} object
     */
    Thread getThread(int threadId);

    /**
     * Returns the stack frames for the specified thread.
     *
     * @param threadId the ID of the thread
     * @param startFrame the index of the first frame to return; if omitted frames start at 0.
     * @param levels the maximum number of frames to return. If levels is not specified or 0, all frames are returned.
     * @return an array of {@link StackFrame} objects and the total frames
     */
    StackTraceResponse getStackFrames(int threadId, Integer startFrame, Integer levels);

    /**
     * Returns the variable scopes for the given stack frame.
     *
     * @param frameId the ID of the stack frame
     * @return an array of {@link Scope} objects
     */
    Scope[] getScopes(int frameId);

    /**
     * Retrieves the variables for a given variable reference.
     *
     * @param variablesReference the reference ID for the variable
     * @return an array of child {@link Variable} objects
     */
    Variable[] getVariables(int variablesReference);

    /**
     * Evaluates the given expression in the context of the specified frame.
     *
     * @param frameId the ID of the stack frame (nullable)
     * @param expression the expression to evaluate
     * @param context the evaludation context
     * @return a {@link CompletableFuture} that completes with the evaluation result
     */
    CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression, String context);

    /**
     * Terminates the debugging session.
     */
    void terminate();

    /**
     * Performs a "step in" operation for the given thread.
     *
     * @param threadId the ID of the thread
     */
    void stepIn(int threadId);

    /**
     * Performs a "step out" operation for the given thread.
     *
     * @param threadId the ID of the thread
     */
    void stepOut(int threadId);

    /**
     * Performs a "step over" operation for the given thread.
     *
     * @param threadId the ID of the thread
     */
    void stepOver(int threadId);

    /**
     * Moves to the next statement for the given thread (alias for step over).
     *
     * @param threadId the ID of the thread
     */
    void next(int threadId);

    /**
     * Provides code completions for the specified context.
     *
     * @param args the {@link CompletionsArguments} for the current context
     * @return a {@link CompletableFuture<CompletionsResponse>} with the suggested completions
     */
    CompletableFuture<CompletionsResponse> completions(CompletionsArguments args);

    /**
     * Returns the source code template for a given source reference.
     *
     * @param sourceReference the source reference
     *
     * @return the source code template for a given source reference
     */
    SourceResponse getSourceReference(int sourceReference);

    /**
     * Registers a debugger listener to receive debug events.
     *
     * @param listener the listener to register
     */
    void addDebuggerListener(DebuggerListener listener);

    /**
     * Unregisters a previously registered debugger listener.
     *
     * @param listener the listener to remove
     */
    void removeDebuggerListener(DebuggerListener listener);

    /**
     * Enables or disables the debugger.
     *
     * @param enabled {@code true} to enable the debugger, {@code false} to disable it
     */
    void setEnabled(boolean enabled);

    /**
     * Returns whether the debugger is currently enabled.
     *
     * @return {@code true} if enabled, {@code false} otherwise
     */
    boolean isEnabled();

}
