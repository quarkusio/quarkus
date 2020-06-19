package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.RequestContext;
import io.quarkus.qrs.runtime.core.RestHandler;

public class ResponseHandler implements RestHandler {
    @Override
    public void handle(RequestContext requestContext) throws Exception {
        requestContext.getContext().response().end(requestContext.getResult().toString());
    }
}
