package org.jboss.resteasy.reactive.server.spi;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.spi.RestHandler;

public interface ServerRestHandler extends RestHandler<ResteasyReactiveRequestContext> {

    void handle(ResteasyReactiveRequestContext requestContext) throws Exception;

}
