package io.quarkus.qrs.runtime.handlers;

import io.quarkus.qrs.runtime.core.QrsRequestContext;

/**
 * Our job is to send initial headers for the SSE request
 */
public class SseResponseWriterHandler implements RestHandler {

    public SseResponseWriterHandler() {
    }

    @Override
    public void handle(QrsRequestContext requestContext) throws Exception {
        requestContext.getSseEventSink().sendInitialResponse(requestContext.getContext().response());
    }
}
