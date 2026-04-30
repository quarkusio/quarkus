package io.quarkus.devui.deployment.exception;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.deployment.IsDevelopment;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.dev.ExceptionNotificationBuildItem;
import io.quarkus.deployment.dev.RuntimeUpdatesProcessor;
import io.quarkus.devui.spi.buildtime.BuildTimeActionBuildItem;
import io.quarkus.devui.spi.buildtime.DevMcpBuildTimeTool;
import io.quarkus.devui.spi.buildtime.DevMcpParam;

/**
 * Exposes the last exception (compilation, deployment, or runtime) as a Dev MCP tool.
 */
@DevMcpBuildTimeTool(name = "getLastException", description = "Get the last exception that occurred in this Quarkus application. Returns compilation errors, deployment failures, hot-reload problems, or runtime exceptions with full stack traces and source locations.", params = {
        @DevMcpParam(name = "maxCauseDepth", description = "Maximum depth for the cause chain (default 5)", required = false)
})
@DevMcpBuildTimeTool(name = "clearLastException", description = "Clear the last recorded runtime exception.")
public class ExceptionProcessor {

    private static final String NAMESPACE = "devui-exceptions";
    static final int MAX_STACK_TRACE_LINES = 200;
    static final int MAX_CAUSE_MESSAGE_LENGTH = 500;
    static final int MAX_CAUSE_DEPTH = 5;

    // Immutable snapshot of a runtime exception and its context
    record ExceptionSnapshot(Throwable exception, StackTraceElement userCodeLocation) {
    }

    // Static so state survives hot reloads (build steps re-execute on reload).
    // Single AtomicReference ensures atomic read/write of all fields together.
    private static final AtomicReference<ExceptionSnapshot> lastRuntimeSnapshot = new AtomicReference<>();

    @BuildStep(onlyIf = IsDevelopment.class)
    void registerExceptionActions(
            BuildProducer<BuildTimeActionBuildItem> buildTimeActionProducer,
            BuildProducer<ExceptionNotificationBuildItem> exceptionNotificationProducer) {

        exceptionNotificationProducer.produce(new ExceptionNotificationBuildItem((throwable, userCode) -> {
            lastRuntimeSnapshot.set(new ExceptionSnapshot(throwable, userCode));
        }));

        BuildTimeActionBuildItem actions = new BuildTimeActionBuildItem(NAMESPACE);

        actions.actionBuilder()
                .methodName("getLastException")
                .description(
                        "Get the last exception that occurred in this Quarkus application. Returns compilation errors, deployment failures, hot-reload problems, or runtime exceptions with full stack traces and source locations. Deployment problems clear automatically on the next successful reload. For runtime exceptions, call clearLastException after handling to avoid seeing the same exception again.")
                .enableMcpFunctionByDefault()
                .parameter("maxCauseDepth",
                        "Maximum depth for the cause chain (default " + MAX_CAUSE_DEPTH + ")")
                .function(params -> {
                    int causeDepth = MAX_CAUSE_DEPTH;
                    String maxCauseDepthParam = params.get("maxCauseDepth");
                    if (maxCauseDepthParam != null && !maxCauseDepthParam.isBlank()) {
                        try {
                            int parsed = Integer.parseInt(maxCauseDepthParam);
                            if (parsed > 0 && parsed <= MAX_CAUSE_DEPTH) {
                                causeDepth = parsed;
                            }
                        } catch (NumberFormatException e) {
                            // LLM may pass non-numeric values like "three"; fall back to default
                        }
                    }

                    // Check deployment problems (compilation, deployment, or hot-reload)
                    Throwable deploymentProblem = null;
                    if (RuntimeUpdatesProcessor.INSTANCE != null) {
                        deploymentProblem = RuntimeUpdatesProcessor.INSTANCE.getDeploymentProblem();
                    }

                    // Check runtime exceptions (single atomic read)
                    ExceptionSnapshot snapshot = lastRuntimeSnapshot.get();

                    if (deploymentProblem == null && snapshot == null) {
                        return Map.of("hasException", false, "message", "No exceptions recorded");
                    }

                    Map<String, Object> result = new HashMap<>();
                    result.put("hasException", true);

                    // Prefer deployment problems as they're more critical
                    if (deploymentProblem != null) {
                        result.put("source", "deployment");
                        populateExceptionDetails(result, deploymentProblem, causeDepth);
                        if (snapshot != null) {
                            result.put("hasRuntimeException", true);
                        }
                    } else {
                        result.put("source", "runtime");
                        populateExceptionDetails(result, snapshot.exception(), causeDepth);
                        if (snapshot.userCodeLocation() != null) {
                            StackTraceElement userCode = snapshot.userCodeLocation();
                            result.put("userCodeLocation",
                                    userCode.getClassName() + "." + userCode.getMethodName()
                                            + "(" + userCode.getFileName() + ":" + userCode.getLineNumber() + ")");
                        }
                    }

                    return result;
                })
                .build();

        actions.actionBuilder()
                .methodName("clearLastException")
                .description("Clear the last recorded runtime exception.")
                .enableMcpFunctionByDefault()
                .function(ignored -> {
                    lastRuntimeSnapshot.set(null);
                    return Map.of("cleared", true);
                })
                .build();

        buildTimeActionProducer.produce(actions);
    }

    static void populateExceptionDetails(Map<String, Object> result, Throwable throwable, int maxCauseDepth) {
        result.put("exceptionClass", throwable.getClass().getName());
        result.put("message", throwable.getMessage());

        String stackTrace;
        try (StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
            pw.flush();
            stackTrace = sw.toString();
        } catch (IOException e) {
            // StringWriter.close() is a no-op; this cannot happen
            throw new RuntimeException(e);
        }

        // Truncate very long stack traces
        String[] lines = stackTrace.split("\n");
        if (lines.length > MAX_STACK_TRACE_LINES) {
            StringBuilder truncated = new StringBuilder();
            for (int i = 0; i < MAX_STACK_TRACE_LINES; i++) {
                truncated.append(lines[i]).append('\n');
            }
            truncated.append("... ").append(lines.length - MAX_STACK_TRACE_LINES).append(" more lines truncated");
            stackTrace = truncated.toString();
        }
        result.put("stackTrace", stackTrace);

        // Include cause chain summary
        Throwable cause = throwable.getCause();
        if (cause != null) {
            Set<Throwable> seen = new HashSet<>();
            seen.add(throwable);
            StringBuilder causeSummary = new StringBuilder();
            int depth = 0;
            // seen.add() returns false if already present, guarding against circular cause chains
            while (cause != null && depth < maxCauseDepth && seen.add(cause)) {
                if (depth > 0) {
                    causeSummary.append(" <- ");
                }
                causeSummary.append(cause.getClass().getSimpleName());
                if (cause.getMessage() != null) {
                    String msg = cause.getMessage();
                    if (msg.length() > MAX_CAUSE_MESSAGE_LENGTH) {
                        msg = msg.substring(0, MAX_CAUSE_MESSAGE_LENGTH) + "...";
                    }
                    causeSummary.append(": ").append(msg);
                }
                cause = cause.getCause();
                depth++;
            }
            result.put("causeChain", causeSummary.toString());
        }
    }
}
