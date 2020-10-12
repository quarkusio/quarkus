package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

/**
 * Our job is to turn an exception into a Response instance. This is only present in the abort chain
 */
public class ExceptionHandler implements RestHandler {

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.mapExceptionIfPresent();
    }
}
