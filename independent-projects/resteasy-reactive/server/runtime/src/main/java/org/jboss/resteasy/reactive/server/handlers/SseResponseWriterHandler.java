package org.jboss.resteasy.reactive.server.handlers;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;

/**
 * Our job is to send initial headers for the SSE request
 */
public class SseResponseWriterHandler implements ServerRestHandler {

    public SseResponseWriterHandler() {
    }

    @Override
    public void handle(ResteasyReactiveRequestContext requestContext) throws Exception {
        requestContext.getSseEventSink().sendInitialResponse(requestContext.getHttpServerResponse());
    }
}
