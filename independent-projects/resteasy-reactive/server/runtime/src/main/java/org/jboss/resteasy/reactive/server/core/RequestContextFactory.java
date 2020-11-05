package org.jboss.resteasy.reactive.server.core;

import io.vertx.ext.web.RoutingContext;
import org.jboss.resteasy.reactive.common.runtime.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;

public interface RequestContextFactory {

    ResteasyReactiveRequestContext createContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers,
            RoutingContext context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain);
}
