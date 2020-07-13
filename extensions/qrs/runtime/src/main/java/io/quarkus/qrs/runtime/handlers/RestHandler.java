package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.RequestContext;

public interface RestHandler {

    void handle(RequestContext requestContext) throws Exception;

}
