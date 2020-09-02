package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.vertx.core.http.HttpServerResponse;

/**
 * Our job is to send initial headers for the SSE request
 */
public class SseResponseWriterHandler implements RestHandler {

    public SseResponseWriterHandler() {
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        HttpServerResponse response = requestContext.getContext().response();
        requestContext.getSseEventSink().sendInitialResponse(response);
    }
}
