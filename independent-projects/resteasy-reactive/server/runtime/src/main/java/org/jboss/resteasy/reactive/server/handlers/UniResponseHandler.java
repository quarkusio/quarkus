package org.jboss.resteasy.reactive.server.handlers;

import io.smallrye.mutiny.Uni;
import java.util.Map;
import java.util.function.Consumer;
import javax.ws.rs.core.Response;
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
                    requestContext.serverResponse().setStatusCode(
                            requestContext.getResponseStatus() != null ? requestContext.getResponseStatus()
                                    : Response.Status.OK.getStatusCode());
                    if (requestContext.getResponseHeaders() != null) {
                        for (Map.Entry<String, String> header : requestContext.getResponseHeaders().entrySet()) {
                            requestContext.serverResponse().addResponseHeader(header.getKey(), header.getValue());
                        }
                    }
                    requestContext.resume();
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable t) {
                    requestContext.resume(t, true);
                }
            });
        }
    }
}
