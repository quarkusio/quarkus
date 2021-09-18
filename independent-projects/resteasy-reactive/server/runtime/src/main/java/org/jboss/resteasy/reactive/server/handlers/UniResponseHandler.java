package org.jboss.resteasy.reactive.server.handlers;

import io.smallrye.mutiny.Uni;
import java.util.function.Consumer;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class UniResponseHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        // FIXME: handle Response with entity being a Uni
        if (requestContext.getResult() instanceof Uni) {
            Uni<?> result = (Uni<?>) requestContext.getResult();
            requestContext.suspend();

            result.subscribe().with(new Consumer<Object>() {
                @Override
                public void accept(Object v) {
                    requestContext.setResult(v);
                    requestContext.resume();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    requestContext.serverResponse().clearResponseHeaders();
                    requestContext.resume(t, true);
                }
            });
        }
    }
}
