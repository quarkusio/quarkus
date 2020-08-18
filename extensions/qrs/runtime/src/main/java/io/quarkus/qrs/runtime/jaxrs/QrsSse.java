package io.quarkus.qrs.runtime.jaxrs;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseBroadcaster;

public class QrsSse implements Sse {

    public static final QrsSse INSTANCE = new QrsSse();

    @Override
    public OutboundSseEvent.Builder newEventBuilder() {
        return new QrsOutboundSseEvent.BuilderImpl();
    }

    @Override
    public SseBroadcaster newBroadcaster() {
        return null;
    }
}
