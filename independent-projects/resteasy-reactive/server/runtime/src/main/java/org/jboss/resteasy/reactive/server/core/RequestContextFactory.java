package org.jboss.resteasy.reactive.server.core;

import org.jboss.resteasy.reactive.common.core.ThreadSetupAction;
import org.jboss.resteasy.reactive.server.handlers.ServerRestHandler;
import org.jboss.resteasy.reactive.server.jaxrs.QuarkusRestProviders;

public interface RequestContextFactory {

    ResteasyReactiveRequestContext createContext(QuarkusRestDeployment deployment, QuarkusRestProviders providers,
            Object context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain);
}
