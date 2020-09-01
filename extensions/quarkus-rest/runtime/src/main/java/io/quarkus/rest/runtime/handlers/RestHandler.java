package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

public interface RestHandler {

    void handle(QuarkusRestRequestContext requestContext) throws Exception;

}
