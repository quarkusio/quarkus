package org.jboss.resteasy.reactive.server.jaxrs;

import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEventSink;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.SseUtil;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

public class SseEventSinkImpl implements SseEventSink {

    public static final byte[] EMPTY_BUFFER = new byte[0];
    private ResteasyReactiveRequestContext context;
    private SseBroadcasterImpl broadcaster;
    private boolean closed;

    public SseEventSinkImpl(ResteasyReactiveRequestContext context) {
        this.context = context;
    }

    @Override
    public synchronized boolean isClosed() {
        return context.serverResponse().closed() || closed;
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        if (isClosed())
            throw new IllegalStateException("Already closed");
        // NOTE: we can't cast event to OutboundSseEventImpl because the TCK sends us its own subclass
        return SseUtil.send(context, event, Collections.emptyList());
    }

    @Override
    public synchronized void close() {
        if (closed)
            return;
        closed = true;
        ServerHttpResponse response = context.serverResponse();
        if (!response.closed()) {
            if (!response.headWritten()) {
                // make sure we send the headers if we're closing this sink before the
                // endpoint method is over
                SseUtil.setHeaders(context, response);
            }
            response.end();
            context.close();
        }
        if (broadcaster != null)
            broadcaster.fireClose(this);
    }

    public void sendInitialResponse(ServerHttpResponse response) {
        if (!response.headWritten()) {
            SseUtil.setHeaders(context, response);
            // send the headers over the wire
            context.suspend();
            response.write(EMPTY_BUFFER, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    if (throwable == null) {
                        context.resume();
                    } else {
                        context.resume(throwable);
                    }
                    // I don't think we should be firing the exception on the broadcaster here
                }
            });
        }
        response.addCloseHandler(this::close);
    }

    void register(SseBroadcasterImpl broadcaster) {
        if (this.broadcaster != null)
            throw new IllegalStateException("Already registered on a broadcaster");
        this.broadcaster = broadcaster;
    }
}
