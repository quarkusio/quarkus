package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public class MatrixParamHandler implements ServerRestHandler {
    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        if (requestContext.getPath().contains(";")) {
            requestContext.initPathSegments();
        }
    }
}
