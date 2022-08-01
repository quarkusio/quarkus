package org.jboss.resteasy.reactive.server.handlers;

import java.util.concurrent.CompletionStage;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class CompletionStageResponseHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
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
