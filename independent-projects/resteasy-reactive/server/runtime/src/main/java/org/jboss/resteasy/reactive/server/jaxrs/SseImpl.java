package org.jboss.resteasy.reactive.server.jaxrs;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

public class SseImpl implements Sse {

    public static final SseImpl INSTANCE = new SseImpl();

    @Override
    public OutboundSseEvent.Builder newEventBuilder() {
        return new OutboundSseEventImpl.BuilderImpl();
    }

    @Override
    public SseBroadcaster newBroadcaster() {
        return new SseBroadcasterImpl();
    }
}
