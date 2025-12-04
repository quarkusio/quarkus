package io.quarkus.qute.debug.adapter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.ContinueResponse;
import org.eclipse.lsp4j.debug.DisconnectArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.ExitedEventArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.OutputEventArguments;
import org.eclipse.lsp4j.debug.PauseArguments;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.ScopesResponse;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsResponse;
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsArguments;
import org.eclipse.lsp4j.debug.SetExceptionBreakpointsResponse;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceArguments;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.SourceResponse;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.StoppedEventArguments;
import org.eclipse.lsp4j.debug.TerminateArguments;
import org.eclipse.lsp4j.debug.TerminatedEventArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.ThreadEventArguments;
import org.eclipse.lsp4j.debug.ThreadsResponse;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.VariablesResponse;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;

import io.quarkus.qute.debug.Debugger;
import io.quarkus.qute.debug.DebuggerListener;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.client.EventBasedJavaSourceResolver;
import io.quarkus.qute.debug.client.JavaSourceLocationEventResponse;
import io.quarkus.qute.debug.client.JavaSourceResolver;
import io.quarkus.qute.debug.client.QuteDebugProtocolClient;

/**
 * Debug Adapter Protocol (DAP) server implementation for Qute debugging.
 * <p>
 * This adapter connects the {@link DebuggeeAgent} (responsible for template
 * execution debugging) with an external DAP client, such as VS Code or
 * IntelliJ. It translates events and requests between Qute's internal debugging
 * system and the standardized DAP interface.
 * </p>
 * <p>
 * It supports breakpoints, stack traces, variable inspection, evaluation,
 * stepping, pausing, resuming, and other typical debugging features.
 * </p>
 */
public class DebugServerAdapter implements IDebugProtocolServer {

    /** The underlying Qute debugger agent. */
    private final Debugger agent;

    /** The connected DAP client (e.g., VS Code). */
    private IDebugProtocolClient client;

    /** Tracks active threads by their IDs. */
    private final Map<Integer, Thread> threads = new HashMap<>();

    /**
     * Creates a new {@link DebugServerAdapter} and registers listeners to forward
     * debugger events to the DAP client.
     *
     * @param agent the Qute debugging agent
     */
    public DebugServerAdapter(DebuggeeAgent agent) {
        this.agent = agent;
        agent.addDebuggerListener(new DebuggerListener() {
            @Override
            public void output(OutputEventArguments args) {
                if (client != null) {
                    client.output(args);
                }
            }

            @Override
            public void onThreadChanged(ThreadEvent event) {
                handleThreadChanged(event);
            }

            @Override
            public void onStopped(StoppedEvent event) {
                handleStopped(event);
            }

            @Override
            public void onTerminate() {
                handleTerminate();
            }
        });
    }

    /**
     * Initializes the debug session, returning supported capabilities.
     *
     * @param args the initialization request arguments
     * @return a future providing the supported capabilities
     */
    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            boolean supportsReverseRequests = args == null || !"vscode".equals(args.getClientID());
            if (!supportsReverseRequests) {
                JavaSourceResolver resolver = ((DebuggeeAgent) agent).getJavaSourceResolver();
                if (resolver != null && !(resolver instanceof EventBasedJavaSourceResolver)) {
                    // Client does not support any known Java source resolution (e.g., VS
                    // Code without reverse support)
                    ((DebuggeeAgent) agent)
                            .setJavaSourceResolver(new EventBasedJavaSourceResolver((QuteDebugProtocolClient) client));
                }
            }
            Capabilities capabilities = new Capabilities();
            capabilities.setSupportsCompletionsRequest(Boolean.TRUE);
            capabilities.setSupportsConditionalBreakpoints(Boolean.TRUE);
            capabilities.setSupportsEvaluateForHovers(Boolean.TRUE);
            return capabilities;
        });
    }

    /**
     * Associates a DAP client with this adapter and configures the appropriate
     * JavaSourceResolver based on the client type.
     *
     * @param client the connected DAP client
     */
    public void connect(IDebugProtocolClient client) {
        this.client = client;
        this.agent.setEnabled(true);

        if (client instanceof JavaSourceResolver javaSourceResolver) {
            // Any client implementing JavaSourceResolver → supports reverse requests
            ((DebuggeeAgent) agent).setJavaSourceResolver(javaSourceResolver);
        }
    }

    /**
     * Attaches to a running debug session.
     *
     * @param args attachment arguments
     * @return a future that completes when the client is notified
     */
    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return CompletableFuture.runAsync(client::initialized);
    }

    /**
     * Sets breakpoints in a given source file.
     *
     * @param args the breakpoints configuration
     * @return a future providing the resolved breakpoints
     */
    @Override
    public CompletableFuture<SetBreakpointsResponse> setBreakpoints(SetBreakpointsArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            SetBreakpointsResponse response = new SetBreakpointsResponse();
            Source source = args.getSource();
            SourceBreakpoint[] sourceBreakpoints = args.getBreakpoints();
            Breakpoint[] breakpoints = agent.setBreakpoints(sourceBreakpoints, source);
            response.setBreakpoints(breakpoints);
            return response;
        });
    }

    /**
     * Required by DAP but not used in this implementation.
     *
     * @param args the exception breakpoints arguments
     * @return an empty response
     */
    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(
            SetExceptionBreakpointsArguments args) {
        return CompletableFuture.supplyAsync(SetExceptionBreakpointsResponse::new);
    }

    /**
     * Returns the list of threads currently being debugged.
     *
     * @return a future containing the threads
     */
    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        return CompletableFuture.supplyAsync(() -> {
            ThreadsResponse response = new ThreadsResponse();
            response.setThreads(agent.getThreads());
            return response;
        });
    }

    /**
     * Returns the stack trace for a given thread.
     *
     * @param args the stack trace request arguments
     * @return a future containing the stack trace
     */
    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            return agent.getStackFrames(args.getThreadId(), args.getStartFrame(), args.getLevels());
        });
    }

    /**
     * Returns the variable scopes for a given stack frame.
     *
     * @param args the scopes request arguments
     * @return a future containing the scopes
     */
    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            ScopesResponse response = new ScopesResponse();
            int frameId = args.getFrameId();
            response.setScopes(agent.getScopes(frameId));
            return response;
        });
    }

    /**
     * Returns the variables for a given variable reference.
     *
     * @param args the variables request arguments
     * @return a future containing the variables
     */
    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            VariablesResponse response = new VariablesResponse();
            int variablesReference = args.getVariablesReference();
            response.setVariables(agent.getVariables(variablesReference));
            return response;
        });
    }

    /**
     * Terminates the current debugging session.
     *
     * @param args the terminate arguments
     * @return a future that completes when the session is terminated
     */
    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        return CompletableFuture.runAsync(agent::terminate);
    }

    /**
     * Disconnects the debug client and disables the agent.
     *
     * @param args the disconnect arguments
     * @return a future that completes when the disconnection is done
     */
    @Override
    public CompletableFuture<Void> disconnect(DisconnectArguments args) {
        return CompletableFuture.runAsync(() -> {
            try {
                agent.terminate();
            } finally {
                this.agent.setEnabled(false);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return CompletableFuture.runAsync(() -> agent.stepIn(args.getThreadId()));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return CompletableFuture.runAsync(() -> agent.stepOut(args.getThreadId()));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return CompletableFuture.runAsync(() -> agent.pause(args.getThreadId()));
    }

    /** {@inheritDoc} */
    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        return CompletableFuture.runAsync(() -> agent.next(args.getThreadId()));
    }

    /**
     * Resumes execution of a specific thread or all threads.
     *
     * @param args the continue request arguments
     * @return a future containing the response
     */
    @Override
    public CompletableFuture<ContinueResponse> continue_(ContinueArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            ContinueResponse response = new ContinueResponse();
            int threadId = args.getThreadId();
            if (threadId != 0) {
                response.setAllThreadsContinued(Boolean.FALSE);
                agent.resume(threadId);
            } else {
                response.setAllThreadsContinued(Boolean.TRUE);
            }
            return response;
        });
    }

    /**
     * Evaluates an expression in the context of a given stack frame.
     *
     * @param args the evaluation arguments
     * @return a future containing the evaluation result
     */
    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return agent.evaluate(args.getFrameId(), args.getExpression(), args.getContext());
    }

    /**
     * Provides code completions for the given context.
     *
     * @param args the completions request arguments
     * @return a future containing the completion results
     */
    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return agent.completions(args);
    }

    @Override
    public CompletableFuture<SourceResponse> source(SourceArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            var source = args.getSource();
            return agent.getSourceReference(
                    source != null && source.getSourceReference() != null ? source.getSourceReference()
                            : args.getSourceReference());
        });
    }

    /**
     * Handles a stopped event and notifies the client.
     *
     * @param event the stopped event
     */
    private void handleStopped(StoppedEvent event) {
        int threadId = event.getThreadId();
        String reason = event.getReason();
        sendStopEvent(threadId, reason);
    }

    /**
     * Handles a thread event and notifies the client.
     *
     * @param event the thread event
     */
    public void handleThreadChanged(ThreadEvent event) {
        int threadId = event.getThreadId();
        String reason = event.getReason();
        sendThreadEvent(threadId, reason);
    }

    /**
     * Sends a stopped notification to the client.
     *
     * @param threadId the thread ID
     * @param reason the reason for stopping
     */
    private void sendStopEvent(int threadId, String reason) {
        if (client == null) {
            return;
        }
        StoppedEventArguments args = new StoppedEventArguments();
        args.setThreadId(threadId);
        args.setReason(reason);
        client.stopped(args);
    }

    /**
     * Sends a thread state change notification to the client.
     *
     * @param threadId the thread ID
     * @param reason the reason for the change
     */
    private void sendThreadEvent(int threadId, String reason) {
        if (client == null) {
            return;
        }
        ThreadEventArguments args = new ThreadEventArguments();
        args.setThreadId(threadId);
        args.setReason(reason);
        client.thread(args);
    }

    /**
     * Handles a terminate event and notifies the client.
     */
    public void handleTerminate() {
        sendExitEvent();
    }

    /**
     * Sends an exit event to the client.
     */
    private void sendExitEvent() {
        if (client == null) {
            return;
        }
        ExitedEventArguments args = new ExitedEventArguments();
        client.exited(args);
        client.terminated(new TerminatedEventArguments());
    }

    @JsonRequest("qute/onJavaSourceResolved")
    public CompletableFuture<Void> onJavaSourceResolved(JavaSourceLocationEventResponse response) {
        JavaSourceResolver resolver = ((DebuggeeAgent) agent).getJavaSourceResolver();
        if (resolver instanceof EventBasedJavaSourceResolver eventBasedJavaSourceResolver) {
            eventBasedJavaSourceResolver.handleResponse(response);
        }
        return CompletableFuture.completedFuture(null);
    }
}
