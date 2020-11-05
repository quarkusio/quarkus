package org.jboss.resteasy.reactive.server.jaxrs;

import io.netty.buffer.Unpooled;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.core.SseUtil;

public class QuarkusRestSseEventSink implements SseEventSink {

    private static final Buffer EMPTY_BUFFER = Buffer.buffer(Unpooled.EMPTY_BUFFER);
    private ResteasyReactiveRequestContext context;
    private QuarkusRestSseBroadcasterImpl broadcaster;

    public QuarkusRestSseEventSink(ResteasyReactiveRequestContext context) {
        this.context = context;
    }

    @Override
    public boolean isClosed() {
        return context.getHttpServerResponse().closed();
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        if (isClosed())
            throw new IllegalStateException("Already closed");
        // NOTE: we can't cast event to QuarkusRestOutboundSseEvent because the TCK sends us its own subclass
        CompletionStage<?> ret = SseUtil.send(context, event);
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
    public void close() {
        if (isClosed())
            return;
        // FIXME: do we need a state flag?
        HttpServerResponse response = context.getHttpServerResponse();
        response.end();
        response.close();
        context.close();
        if (broadcaster != null)
            broadcaster.fireClose(this);
    }

    public void sendInitialResponse(HttpServerResponse response) {
        if (!response.headWritten()) {
            SseUtil.setHeaders(context, response);
            // send the headers over the wire
            context.suspend();
            response.write(EMPTY_BUFFER, new Handler<AsyncResult<Void>>() {
                @Override
                public void handle(AsyncResult<Void> event) {
                    if (event.succeeded())
                        context.resume();
                    else
                        context.resume(event.cause());
                    // I don't think we should be firing the exception on the broadcaster here
                }
            });

            response.closeHandler(v -> {
                // FIXME: notify of client closing
                System.err.println("Server connection closed");
            });
        }
    }

    void register(QuarkusRestSseBroadcasterImpl broadcaster) {
        if (this.broadcaster != null)
            throw new IllegalStateException("Already registered on a broadcaster");
        this.broadcaster = broadcaster;
    }
}
