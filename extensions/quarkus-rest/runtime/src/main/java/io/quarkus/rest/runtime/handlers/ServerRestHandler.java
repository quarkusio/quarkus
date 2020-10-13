package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public interface ServerRestHandler extends RestHandler<QuarkusRestRequestContext> {

    void handle(QuarkusRestRequestContext requestContext) throws Exception;

}
