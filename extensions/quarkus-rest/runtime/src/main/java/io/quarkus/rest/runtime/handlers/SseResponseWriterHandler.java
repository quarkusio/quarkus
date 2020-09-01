package io.quarkus.rest.runtime.handlers;

import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;

/**
 * Our job is to send initial headers for the SSE request
 */
public class SseResponseWriterHandler implements RestHandler {

    public SseResponseWriterHandler() {
    }

    @Override
    public void handle(QuarkusRestRequestContext requestContext) throws Exception {
        requestContext.getSseEventSink().sendInitialResponse(requestContext.getContext().response());
    }
}
