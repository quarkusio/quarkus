package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

public class NonBlockingHandler implements ServerRestHandler {

    public static final NonBlockingHandler INSTANCE = new NonBlockingHandler();

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        if (requestContext.serverRequest().isOnIoThread()) {
            return;
        }
        requestContext.suspend();
        requestContext.resume(requestContext.getContextExecutor());
    }
}
