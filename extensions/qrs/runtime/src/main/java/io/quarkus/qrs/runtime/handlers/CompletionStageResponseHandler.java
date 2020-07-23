package io.quarkus.qrs.runtime.handlers;

import java.util.concurrent.CompletionStage;

import io.quarkus.qrs.runtime.core.RequestContext;

public class CompletionStageResponseHandler implements RestHandler {

    @Override
    public void handle(RequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a CompletionStage
        if (requestContext.getResult() instanceof CompletionStage) {
            CompletionStage<?> result = (CompletionStage<?>) requestContext.getResult();
            requestContext.suspend();

            result.handle((v, t) -> {
                if (t != null) {
                    requestContext.setThrowable(t);
                } else {
                    requestContext.setResult(v);
                }
                requestContext.resume();
                return null;
            });
        }
    }
}
