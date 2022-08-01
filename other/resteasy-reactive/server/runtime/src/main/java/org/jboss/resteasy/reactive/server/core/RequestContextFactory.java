package org.jboss.resteasy.reactive.server.core;

import org.jboss.resteasy.reactive.server.jaxrs.ProvidersImpl;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;
import org.jboss.resteasy.reactive.spi.ThreadSetupAction;

public interface RequestContextFactory {

    ResteasyReactiveRequestContext createContext(Deployment deployment, ProvidersImpl providers,
            Object context,
            ThreadSetupAction requestContext, ServerRestHandler[] handlerChain, ServerRestHandler[] abortHandlerChain);

    /**
     * @return <code>true</code> if requests default to blocking when created by this factory
     */
    default boolean isDefaultBlocking() {
        return false;
    }

}
