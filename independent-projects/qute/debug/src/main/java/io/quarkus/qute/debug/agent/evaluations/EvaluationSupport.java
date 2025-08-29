package io.quarkus.qute.debug.agent.evaluations;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.EvaluateArgumentsContext;
import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesHelper;

public class EvaluationSupport {

    private static final Object IGNORE_RESULT = new Object();
    private final DebuggeeAgent agent;

    public EvaluationSupport(DebuggeeAgent agent) {
        this.agent = agent;
    }

    public CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression, String context) {
        if (!agent.isEnabled()) {
            ResponseError re = new ResponseError();
            re.setCode(ResponseErrorCode.InvalidRequest);
            re.setMessage("Debuggee agent is not enabled.");
            throw new ResponseErrorException(re);
        }
        RemoteStackFrame frame = agent.findStackFrame(frameId);
        if (frame == null) {
            return CompletableFuture.completedFuture(null);
        }
        return frame.evaluate(expression)//
                .handle((result, error) -> {
                    if (error != null) {
                        if (EvaluateArgumentsContext.HOVER.equals(context)) {
                            return IGNORE_RESULT;
                        }
                        ResponseError re = new ResponseError();
                        re.setCode(ResponseErrorCode.InvalidRequest);
                        re.setMessage(error.getMessage());
                        throw new ResponseErrorException(re);
                    }
                    return result;
                }).thenApply(result -> {
                    EvaluateResponse response = new EvaluateResponse();
                    if (result != null) {
                        if (result != IGNORE_RESULT) {
                            response.setResult(result.toString());
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
