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

    public QuarkusRestSseEventSink(QuarkusRestRequestContext context) {
        this.context = context;
    }

    @Override
    public boolean isClosed() {
        return context.getContext().response().closed();
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        return SseUtil.send(context, event);
    }

    @Override
    public void close() {
        if (isClosed())
            return;
        // FIXME: close
        // FIXME: should we close too?
        //        context.getContext().response().close();
        context.getContext().response().end();
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
                }
            });

            response.closeHandler(v -> {
                System.err.println("Server connection closed");
            });
        }
    }
}
