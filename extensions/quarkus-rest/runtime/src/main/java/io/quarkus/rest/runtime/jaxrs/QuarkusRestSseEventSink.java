package io.quarkus.rest.runtime.jaxrs;

import java.util.concurrent.CompletionStage;

import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import io.netty.buffer.Unpooled;
import io.quarkus.rest.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.runtime.core.SseUtil;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class QuarkusRestSseEventSink implements SseEventSink {

    private static final Buffer EMPTY_BUFFER = Buffer.buffer(Unpooled.EMPTY_BUFFER);
    private QuarkusRestRequestContext context;
    private QuarkusRestSseBroadcasterImpl broadcaster;

    public QuarkusRestSseEventSink(QuarkusRestRequestContext context) {
        this.context = context;
    }

    @Override
    public boolean isClosed() {
        return context.getContext().response().closed();
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        if (isClosed())
            throw new IllegalStateException("Already closed");
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
        context.getContext().response().end();
        context.getContext().response().close();
        context.close();
        if (broadcaster != null)
            broadcaster.fireClose(this);
    }

    public void sendInitialResponse(HttpServerResponse response) {
        if (!response.headWritten()) {
            SseUtil.setHeaders(response);
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
