package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Our job is to turn an exception into a Response instance. This is only present in the abort chain
 */
public class ExceptionHandler implements ServerRestHandler {

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.serverResponse().clearResponseHeaders();
        requestContext.mapExceptionIfPresent();
    }
}
