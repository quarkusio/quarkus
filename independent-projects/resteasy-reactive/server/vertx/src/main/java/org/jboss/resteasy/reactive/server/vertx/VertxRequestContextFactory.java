package org.jboss.resteasy.reactive.server.vertx;

import org.jboss.resteasy.reactive.server.core.Deployment;
import org.jboss.resteasy.reactive.server.core.RequestContextFactory;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

import io.vertx.ext.web.RoutingContext;

public class VertxRequestContextFactory implements RequestContextFactory {
    @Override
    public ResteasyReactiveRequestContext createContext(Deployment deployment, Object context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain) {
        return new VertxResteasyReactiveRequestContext(deployment, (RoutingContext) context, requestContext,
                handlerChain, abortHandlerChain, null);
    }
}
