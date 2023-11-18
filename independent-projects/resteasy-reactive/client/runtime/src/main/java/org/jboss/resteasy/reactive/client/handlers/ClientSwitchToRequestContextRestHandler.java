package org.jboss.resteasy.reactive.client.handlers;

import java.util.concurrent.Executor;

import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;

/**
 * This handler ensures that the context to use is the same as the client request context, which is important to keep the
 * request context in sync when updating the response.
 */
public class ClientSwitchToRequestContextRestHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        Context current = Vertx.currentContext();
        ClientRequestContextImpl clientRequestContext = requestContext.getClientRequestContext();
        if (clientRequestContext == null) {
            return;
        }

        Context captured = clientRequestContext.getContext();
        if (captured != null && current != captured) {
            requestContext.suspend();
            requestContext.resume(command -> captured.runOnContext(unused -> command.run()));
        }
    }
}
