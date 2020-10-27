package io.quarkus.rest.server.runtime.handlers;

import java.util.concurrent.CompletionStage;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

public class CompletionStageResponseHandler implements ServerRestHandler {

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a CompletionStage
        if (requestContext.getResult() instanceof CompletionStage) {
            CompletionStage<?> result = (CompletionStage<?>) requestContext.getResult();
            requestContext.suspend();

            result.handle((v, t) -> {
                if (t != null) {
                    requestContext.handleException(t);
                } else {
                    requestContext.setResult(v);
                }
                requestContext.resume();
                return null;
            });
        }
    }
}
