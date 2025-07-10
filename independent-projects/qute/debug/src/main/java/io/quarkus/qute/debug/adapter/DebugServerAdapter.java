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
import org.eclipse.lsp4j.debug.SourceBreakpoint;
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

import io.quarkus.qute.debug.Debugger;
import io.quarkus.qute.debug.DebuggerListener;
import io.quarkus.qute.debug.StoppedEvent;
import io.quarkus.qute.debug.ThreadEvent;
import io.quarkus.qute.debug.agent.DebuggeeAgent;

/**
 * Adapter implementing the Debug Adapter Protocol (DAP) server interface. It
 * connects the Qute debugging agent ({@link DebuggeeAgent}) with a DAP client
 * such as VS Code or any LSP-compatible IDE, allowing communication via
 * standard DAP messages.
 */
public class DebugServerAdapter implements IDebugProtocolServer {

    private final Debugger agent;
    private IDebugProtocolClient client;

    private final Map<Integer, Thread> threads = new HashMap<>();

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

    @Override
    public CompletableFuture<Capabilities> initialize(InitializeRequestArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            Capabilities capabilities = new Capabilities();
            capabilities.setSupportsCompletionsRequest(Boolean.TRUE);
            capabilities.setSupportsConditionalBreakpoints(Boolean.TRUE);
            return capabilities;
        });
    }

    public void connect(IDebugProtocolClient client) {
        this.client = client;
        this.agent.setEnabled(true);
    }

    @Override
    public CompletableFuture<Void> attach(Map<String, Object> args) {
        return CompletableFuture.runAsync(() -> {
            client.initialized();
        });
    }

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

    @Override
    public CompletableFuture<SetExceptionBreakpointsResponse> setExceptionBreakpoints(
            SetExceptionBreakpointsArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            // vscode requires to implement this method.
            SetExceptionBreakpointsResponse response = new SetExceptionBreakpointsResponse();
            return response;
        });
    }

    @Override
    public CompletableFuture<ThreadsResponse> threads() {
        return CompletableFuture.supplyAsync(() -> {
            ThreadsResponse response = new ThreadsResponse();
            response.setThreads(agent.getThreads());
            return response;
        });
    }

    @Override
    public CompletableFuture<StackTraceResponse> stackTrace(StackTraceArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            StackTraceResponse response = new StackTraceResponse();
            int threadId = args.getThreadId();
            var stackFrames = agent.getStackFrames(threadId);
            response.setStackFrames(stackFrames);
            response.setTotalFrames(stackFrames.length);
            return response;
        });
    }

    @Override
    public CompletableFuture<ScopesResponse> scopes(ScopesArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            ScopesResponse response = new ScopesResponse();
            int frameId = args.getFrameId();
            response.setScopes(agent.getScopes(frameId));
            return response;
        });
    }

    @Override
    public CompletableFuture<VariablesResponse> variables(VariablesArguments args) {
        return CompletableFuture.supplyAsync(() -> {
            VariablesResponse response = new VariablesResponse();
            int variablesReference = args.getVariablesReference();
            response.setVariables(agent.getVariables(variablesReference));
            return response;
        });
    }

    @Override
    public CompletableFuture<Void> terminate(TerminateArguments args) {
        return CompletableFuture.runAsync(agent::terminate);
    }

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

    @Override
    public CompletableFuture<Void> stepIn(StepInArguments args) {
        return CompletableFuture.runAsync(() -> {
            agent.stepIn(args.getThreadId());
        });
    }

    @Override
    public CompletableFuture<Void> stepOut(StepOutArguments args) {
        return CompletableFuture.runAsync(() -> {
            agent.stepOut(args.getThreadId());
        });
    }

    @Override
    public CompletableFuture<Void> pause(PauseArguments args) {
        return CompletableFuture.runAsync(() -> {
            agent.pause(args.getThreadId());
        });
    }

    @Override
    public CompletableFuture<Void> next(NextArguments args) {
        return CompletableFuture.runAsync(() -> {
            agent.next(args.getThreadId());
        });
    }

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

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(EvaluateArguments args) {
        return agent.evaluate(args.getFrameId(), args.getExpression());
    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        return agent.completions(args);
    }

    private void handleStopped(StoppedEvent event) {
        int threadId = event.getThreadId();
        String reason = event.getReason();
        sendStopEvent(threadId, reason);
    }

    public void handleThreadChanged(ThreadEvent event) {
        int threadId = event.getThreadId();
        String reason = event.getReason();
        sendThreadEvent(threadId, reason);
    }

    private void sendStopEvent(int threadId, String reason) {
        if (client == null) {
            return;
        }
        StoppedEventArguments args = new StoppedEventArguments();
        args.setThreadId(threadId);
        args.setReason(reason);
        client.stopped(args);
    }

    private void sendThreadEvent(int threadId, String reason) {
        if (client == null) {
            return;
        }
        ThreadEventArguments args = new ThreadEventArguments();
        args.setThreadId(threadId);
        args.setReason(reason);
        client.thread(args);
    }

    public void handleTerminate() {
        sendExitEvent();
    }

    private void sendExitEvent() {
        if (client == null) {
            return;
        }
        ExitedEventArguments args = new ExitedEventArguments();
        client.exited(args);
        client.terminated(new TerminatedEventArguments());
    }
}
