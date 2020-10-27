package io.quarkus.rest.server.runtime.handlers;

import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;

/**
 * Our job is to send initial headers for the SSE request
 */
public class SseResponseWriterHandler implements ServerRestHandler {

    public SseResponseWriterHandler() {
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.getSseEventSink().sendInitialResponse(requestContext.getHttpServerResponse());
    }
}
