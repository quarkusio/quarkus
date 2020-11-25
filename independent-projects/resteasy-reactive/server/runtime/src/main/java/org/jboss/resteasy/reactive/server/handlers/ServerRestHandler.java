package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.common.core.RestHandler;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

public interface ServerRestHandler extends RestHandler<ResteasyReactiveRequestContext> {

    void handle(ResteasyReactiveRequestContext requestContext) throws Exception;

}
