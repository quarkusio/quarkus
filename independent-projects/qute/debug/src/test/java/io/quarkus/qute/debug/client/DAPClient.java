package io.quarkus.qute.debug.client;

import static io.quarkus.qute.debug.agent.completions.CompletionSupport.EMPTY_COMPLETION_ITEMS;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.UnaryOperator;

import org.eclipse.lsp4j.debug.Breakpoint;
import org.eclipse.lsp4j.debug.Capabilities;
import org.eclipse.lsp4j.debug.CompletionItem;
import org.eclipse.lsp4j.debug.CompletionsArguments;
import org.eclipse.lsp4j.debug.CompletionsResponse;
import org.eclipse.lsp4j.debug.ConfigurationDoneArguments;
import org.eclipse.lsp4j.debug.ContinueArguments;
import org.eclipse.lsp4j.debug.EvaluateArguments;
import org.eclipse.lsp4j.debug.EvaluateArgumentsContext;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.debug.InitializeRequestArguments;
import org.eclipse.lsp4j.debug.InitializeRequestArgumentsPathFormat;
import org.eclipse.lsp4j.debug.NextArguments;
import org.eclipse.lsp4j.debug.Scope;
import org.eclipse.lsp4j.debug.ScopesArguments;
import org.eclipse.lsp4j.debug.SetBreakpointsArguments;
import org.eclipse.lsp4j.debug.Source;
import org.eclipse.lsp4j.debug.SourceBreakpoint;
import org.eclipse.lsp4j.debug.SourceResponse;
import org.eclipse.lsp4j.debug.StackFrame;
import org.eclipse.lsp4j.debug.StackTraceArguments;
import org.eclipse.lsp4j.debug.StackTraceResponse;
import org.eclipse.lsp4j.debug.StepInArguments;
import org.eclipse.lsp4j.debug.StepOutArguments;
import org.eclipse.lsp4j.debug.Thread;
import org.eclipse.lsp4j.debug.Variable;
import org.eclipse.lsp4j.debug.VariablesArguments;
import org.eclipse.lsp4j.debug.launch.DSPLauncher;
import org.eclipse.lsp4j.debug.services.IDebugProtocolClient;
import org.eclipse.lsp4j.debug.services.IDebugProtocolServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;

import io.quarkus.qute.debug.Debugger;
import io.quarkus.qute.debug.DebuggerListener;
import io.quarkus.qute.debug.DebuggerState;
import io.quarkus.qute.debug.client.TransportStreams.SocketTransportStreams;

public class DAPClient implements IDebugProtocolClient, Debugger {

    private IDebugProtocolServer debugProtocolServer;
    private Future<Void> debugProtocolFuture;

    private final CompletableFuture<Capabilities> capabilitiesFuture = new CompletableFuture<>();
    private final CompletableFuture<Void> initialized = new CompletableFuture<>();
    private boolean enabled;

    public CompletableFuture<Void> connectToServer(int port) {
        ServerTrace serverTrace = ServerTrace.getDefaultValue();
        TracingMessageConsumer tracing = serverTrace != ServerTrace.off ? new TracingMessageConsumer() : null;
        UnaryOperator<MessageConsumer> wrapper = consumer -> {
            MessageConsumer result = consumer;
            if (tracing != null) {
                result = message -> {
                    // Display DAP message in the console
                    String log = tracing.log(message, consumer, serverTrace);
                    System.err.println(log);
                    consumer.consume(message);
                };
            }
            if (true) {
                // result = new ReflectiveMessageValidator(result);
            }
            return result;
        };

        // Wait for some time to be sure that server socket is ready...
        try {
            java.lang.Thread.sleep(2000);
        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
        }

        SocketTransportStreams transportStreams = new SocketTransportStreams(null, port);
        Launcher<? extends IDebugProtocolServer> debugProtocolLauncher = DSPLauncher.createClientLauncher(this,
                transportStreams.in, transportStreams.out, null, wrapper);

        debugProtocolFuture = debugProtocolLauncher.startListening();
        debugProtocolServer = debugProtocolLauncher.getRemoteProxy();

        Map<String, Object> dapParameters = new HashMap<>();
        return initialize(dapParameters);
    }

    private CompletableFuture<Void> initialize(Map<String, Object> dapParameters) {
        InitializeRequestArguments args = new InitializeRequestArguments();
        args.setPathFormat(InitializeRequestArgumentsPathFormat.PATH);
        args.setSupportsVariableType(true);
        args.setSupportsVariablePaging(false);
        args.setLinesStartAt1(true);
        args.setColumnsStartAt1(true);
        args.setSupportsStartDebuggingRequest(true);

        CompletableFuture<?> attachFuture = getDebugProtocolServer().initialize(args).thenAccept(capabilities -> {
            if (capabilities == null) {
                capabilities = new Capabilities();
            }
            capabilitiesFuture.complete(capabilities);
        }).thenCompose(unused -> {
            return getDebugProtocolServer().attach(dapParameters);
        }).handle((q, t) -> {
            if (t != null) {
                initialized.completeExceptionally(t);
            }
            return q;
        });
        CompletableFuture<Void> configurationDoneFuture = CompletableFuture.allOf(initialized, capabilitiesFuture);
        boolean isDebug = true;
        if (isDebug) {
            configurationDoneFuture = configurationDoneFuture.thenCompose(v -> {
                // client sends zero or more setBreakpoints requests
                return sendBreakpoints();
            }).thenCompose(v -> {
                // client sends a setExceptionBreakpoints request
                // if one or more exceptionBreakpointFilters have been defined
                // (or if supportsConfigurationDoneRequest is not true)
                return CompletableFuture.completedFuture(null);
            });
        }
        configurationDoneFuture = configurationDoneFuture.thenCompose(v -> {
            // client sends one configurationDone request to indicate the end of the
            // configuration.
            if (Boolean.TRUE.equals(getCapabilities().getSupportsConfigurationDoneRequest())) {
                return getDebugProtocolServer().configurationDone(new ConfigurationDoneArguments());
            }
            return CompletableFuture.completedFuture(null);
        });
        return CompletableFuture.allOf(attachFuture, configurationDoneFuture);
    }

    @Override
    public void initialized() {
        initialized.complete(null);
    }

    public void continue_(int threadId) {
        if (debugProtocolServer == null) {
            return;
        }
        ContinueArguments args = new ContinueArguments();
        args.setThreadId(threadId);
        getResult(debugProtocolServer.continue_(args));
    }

    public void next(int threadId) {
        if (debugProtocolServer == null) {
            return;
        }
        NextArguments args = new NextArguments();
        args.setThreadId(threadId);
        getResult(debugProtocolServer.next(args));
    }

    public void stepOut(int threadId) {
        if (debugProtocolServer == null) {
            return;
        }
        StepOutArguments args = new StepOutArguments();
        args.setThreadId(threadId);
        debugProtocolServer.stepOut(args);
    }

    public void stepIn(int threadId) {
        if (debugProtocolServer == null) {
            return;
        }
        StepInArguments args = new StepInArguments();
        args.setThreadId(threadId);
        getResult(debugProtocolServer.stepIn(args));
    }

    @Override
    public CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression, String context) {
        if (debugProtocolServer == null) {
            return CompletableFuture.completedFuture(null);
        }
        EvaluateArguments args = new EvaluateArguments();
        args.setExpression(expression);
        args.setFrameId(frameId);
        args.setContext(context);
        return debugProtocolServer.evaluate(args);
    }

    public EvaluateResponse evaluateSync(int frameId, String expression) {
        return getResult(evaluate(frameId, expression, EvaluateArgumentsContext.WATCH));
    }

    public IDebugProtocolServer getDebugProtocolServer() {
        return debugProtocolServer;
    }

    private Capabilities getCapabilities() {
        return capabilitiesFuture.getNow(new Capabilities());
    }

    private CompletableFuture<Void> sendBreakpoints() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public DebuggerState getState(int threadId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void pause(int threadId) {
        // TODO Auto-generated method stub

    }

    @Override
    public void resume(int threadId) {
        ContinueArguments args = new ContinueArguments();
        args.setThreadId(threadId);
        getDebugProtocolServer().continue_(args);
    }

    public void setBreakpoint(String templatePath, int line) {
        setBreakpoint(templatePath, line, null);
    }

    public void setBreakpoint(String templatePath, int line, String condition) {
        Source source = new Source();
        source.setPath(templatePath);
        SourceBreakpoint sourceBreakpoint = new SourceBreakpoint();
        sourceBreakpoint.setLine(line);
        sourceBreakpoint.setCondition(condition);
        setBreakpoints(new SourceBreakpoint[] { sourceBreakpoint }, source);
    }

    @Override
    public Breakpoint[] setBreakpoints(SourceBreakpoint[] sourceBreakpoints, Source source) {
        SetBreakpointsArguments args = new SetBreakpointsArguments();
        args.setSource(source);
        args.setBreakpoints(sourceBreakpoints);
        return getResult(getDebugProtocolServer().setBreakpoints(args)).getBreakpoints();
    }

    @Override
    public Thread[] getThreads() {
        return getResult(getDebugProtocolServer().threads()).getThreads();
    }

    @Override
    public Thread getThread(int threadId) {
        // TODO Auto-generated method stub
        return null;
    }

    public StackFrame[] getStackFrames(int threadId) {
        return getStackFrames(threadId, null, null).getStackFrames();
    }

    @Override
    public StackTraceResponse getStackFrames(int threadId, Integer startFrame, Integer levels) {
        StackTraceArguments args = new StackTraceArguments();
        args.setThreadId(threadId);
        args.setStartFrame(startFrame);
        args.setLevels(levels);
        return getResult(getDebugProtocolServer().stackTrace(args));
    }

    @Override
    public Scope[] getScopes(int frameId) {
        ScopesArguments args = new ScopesArguments();
        args.setFrameId(frameId);
        return getResult(getDebugProtocolServer().scopes(args)).getScopes();
    }

    @Override
    public Variable[] getVariables(int variablesReference) {
        VariablesArguments args = new VariablesArguments();
        args.setVariablesReference(variablesReference);
        return getResult(getDebugProtocolServer().variables(args)).getVariables();
    }

    @Override
    public void terminate() {
        // TODO Auto-generated method stub

    }

    @Override
    public void stepOver(int threadId) {

    }

    @Override
    public CompletableFuture<CompletionsResponse> completions(CompletionsArguments args) {
        if (debugProtocolServer == null) {
            return CompletableFuture.completedFuture(null);
        }
        return debugProtocolServer.completions(args);
    }

    public CompletionItem[] completion(String text, int line, int column, int frameId) {
        if (debugProtocolServer == null) {
            return EMPTY_COMPLETION_ITEMS;
        }

        CompletionsArguments args = new CompletionsArguments();
        args.setText(text);
        args.setLine(line);
        args.setColumn(column);
        args.setFrameId(frameId);
        var response = getResult(completions(args));
        return response != null ? response.getTargets() : EMPTY_COMPLETION_ITEMS;
    }

    @Override
    public SourceResponse getSourceReference(int sourceReference) {
        return null;
    }

    @Override
    public void addDebuggerListener(DebuggerListener listener) {

    }

    @Override
    public void removeDebuggerListener(DebuggerListener listener) {

    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    private <T> T getResult(CompletableFuture<T> future) {
        try {
            return future.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            java.lang.Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ResponseErrorException) {
                throw (ResponseErrorException) e.getCause();
            }
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

}
