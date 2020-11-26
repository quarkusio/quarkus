package org.jboss.resteasy.reactive.server.handlers;

import io.smallrye.mutiny.Uni;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class UniResponseHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
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
