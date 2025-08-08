package io.quarkus.qute.debug.agent.evaluations;

import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.debug.EvaluateResponse;
import org.eclipse.lsp4j.jsonrpc.ResponseErrorException;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseError;
import org.eclipse.lsp4j.jsonrpc.messages.ResponseErrorCode;

import io.quarkus.qute.debug.agent.DebuggeeAgent;
import io.quarkus.qute.debug.agent.RemoteStackFrame;
import io.quarkus.qute.debug.agent.variables.VariablesHelper;

public class EvaluationSupport {

    private final DebuggeeAgent agent;

    public EvaluationSupport(DebuggeeAgent agent) {
        this.agent = agent;
    }

    public CompletableFuture<EvaluateResponse> evaluate(Integer frameId, String expression) {
        if (!agent.isEnabled()) {
            ResponseError re = new ResponseError();
            re.setCode(ResponseErrorCode.InvalidRequest);
            re.setMessage("Debuggee agent is not enabled.");
            throw new ResponseErrorException(re);
        }
        return doEvaluate(frameId, expression).handle((result, error) -> {
            if (error != null) {
                ResponseError re = new ResponseError();
                re.setCode(ResponseErrorCode.InvalidRequest);
                re.setMessage(error.getMessage());
                throw new ResponseErrorException(re);
            }
            return result;
        }).thenApply(result -> {
            EvaluateResponse response = new EvaluateResponse();
            if (result != null) {
                response.setResult(result.toString());
                if (VariablesHelper.shouldBeExpanded(result)) {
                    var variable = VariablesHelper.fillVariable("", result, null, agent.getVariablesRegistry());
                    response.setVariablesReference(variable.getVariablesReference());
                }
            }
            return response;
        });
    }

    private CompletableFuture<Object> doEvaluate(Integer frameId, String expression) {
        RemoteStackFrame frame = agent.findStackFrame(frameId);
        if (frame != null) {
            return frame.evaluate(expression);
        }
        return CompletableFuture.completedFuture(null);
    }

}
