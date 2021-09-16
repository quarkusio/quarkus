package org.jboss.resteasy.reactive.server.jaxrs;

import java.util.Collections;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.SseUtil;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

public class SseEventSinkImpl implements SseEventSink {

    private static final byte[] EMPTY_BUFFER = new byte[0];
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
        CompletionStage<?> ret = SseUtil.send(context, event, Collections.emptyList());
        if (broadcaster != null) {
            return ret.whenComplete((value, x) -> {
                if (x != null) {
                    broadcaster.fireException(this, x);
                }
            });
        }
        return ret;
    }

    @Override
    public synchronized void close() {
        if (isClosed())
            return;
        closed = true;
        // FIXME: do we need a state flag?
        ServerHttpResponse response = context.serverResponse();
        if (!response.headWritten()) {
            // make sure we send the headers if we're closing this sink before the
            // endpoint method is over
            SseUtil.setHeaders(context, response);
        }
        response.end();
        context.close();
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
            //            response.closeHandler(v -> {
            //                // FIXME: notify of client closing
            //                System.err.println("Server connection closed");
            //            });
        }
    }

    void register(SseBroadcasterImpl broadcaster) {
        if (this.broadcaster != null)
            throw new IllegalStateException("Already registered on a broadcaster");
        this.broadcaster = broadcaster;
    }
}
