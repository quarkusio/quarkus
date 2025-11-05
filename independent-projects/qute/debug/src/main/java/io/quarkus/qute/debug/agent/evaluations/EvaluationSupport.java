package io.quarkus.qute.debug.agent.evaluations;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.EvaluateArgumentsContext;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.agent.frames.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesHelper;

/**
 * Provides support for evaluating expressions in the context of a debugged template.
 * <p>
 * This class is used by the debugger to evaluate expressions in a specific stack frame,
 * such as when the user hovers over a variable or issues an "evaluate" request.
 * </p>
 */
public class EvaluationSupport {

    /** Sentinel value used to ignore evaluation results in hover context. */
    private static final Object IGNORE_RESULT = new Object();

    /** Reference to the debuggee agent that manages threads, frames, and variables. */
    private final DebuggeeAgent agent;

    public EvaluationSupport(DebuggeeAgent agent) {
        this.agent = agent;
    }

    /**
     * Evaluates a string expression in the context of a given stack frame.
     *
     * @param frameId the ID of the stack frame where the expression should be evaluated
     * @param expression the expression to evaluate
     * @param context the evaluation context (e.g., HOVER, WATCH)
     * @return a CompletableFuture resolving to an {@link EvaluateResponse}
     */
    public CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression, String context) {
        if (!agent.isEnabled()) {
            // Debugger not enabled: return an error immediately
            ResponseError re = new ResponseError();
            re.setCode(ResponseErrorCode.InvalidRequest);
            re.setMessage("Debuggee agent is not enabled.");
            throw new ResponseErrorException(re);
        }

        // Find the stack frame
        RemoteStackFrame frame = agent.findStackFrame(frameId);
        if (frame == null) {
            // Frame not found: return null
            return CompletableFuture.completedFuture(null);
        }

        // Evaluate the expression asynchronously
        return frame.evaluate(expression)
                .handle((result, error) -> {
                    // Handle evaluation errors
                    if (error != null) {
                        if (EvaluateArgumentsContext.HOVER.equals(context)) {
                            // Ignore errors in hover context
                            return IGNORE_RESULT;
                        }
                        // Otherwise, propagate as JSON-RPC error
                        ResponseError re = new ResponseError();
                        re.setCode(ResponseErrorCode.InvalidRequest);
                        re.setMessage(error.getMessage());
                        throw new ResponseErrorException(re);
                    }
                    return result;
                })
                .thenApply(result -> {
                    // Build EvaluateResponse
                    EvaluateResponse response = new EvaluateResponse();
                    if (result != null) {
                        if (result != IGNORE_RESULT) {
                            response.setResult(result.toString());
                            // If the result can be expanded (object/array), populate variables reference
                            if (VariablesHelper.shouldBeExpanded(result, frame)) {
                                var variable = VariablesHelper.fillVariable("", result, frame, null,
                                        agent.getVariablesRegistry());
                                response.setVariablesReference(variable.getVariablesReference());
                            }
                        }
                    } else {
                        response.setResult("null");
                    }
                    return response;
                });
    }
}
