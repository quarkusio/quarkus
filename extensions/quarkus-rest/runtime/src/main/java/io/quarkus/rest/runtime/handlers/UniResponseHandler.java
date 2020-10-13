package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.smallrye.mutiny.Uni;

public class UniResponseHandler implements ServerRestHandler {

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a Uni
        if (requestContext.getResult() instanceof Uni) {
            Uni<?> result = (Uni<?>) requestContext.getResult();
            requestContext.suspend();

            result.subscribe().with(v -> {
                requestContext.setResult(v);
                requestContext.resume();
            }, t -> {
                requestContext.handleException(t);
                requestContext.resume();
            });
        }
    }
}
