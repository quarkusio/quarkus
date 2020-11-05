package io.quarkus.rest.server.runtime.handlers;

import org.jboss.resteasy.reactive.common.runtime.core.RestHandler;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

public interface ServerRestHandler extends RestHandler<QuarkusRestRequestContext> {

    void handle(QuarkusRestRequestContext requestContext) throws Exception;

}
