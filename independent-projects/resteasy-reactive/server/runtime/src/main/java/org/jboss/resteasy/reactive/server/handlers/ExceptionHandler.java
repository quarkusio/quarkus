package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ServerRestHandler;

/**
 * Our job is to turn an exception into a Response instance. This is only present in the abort chain
 */
public class ExceptionHandler implements ServerRestHandler {

    public static final ExceptionHandler INSTANCE = new ExceptionHandler();

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.mapExceptionIfPresent();
    }
}
