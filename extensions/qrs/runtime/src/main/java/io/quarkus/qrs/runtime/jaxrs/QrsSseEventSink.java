package io.quarkus.qrs.runtime.jaxrs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEventSink;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class QrsSseEventSink implements SseEventSink {

    private volatile boolean isClosed;
    private QrsRequestContext context;
    private boolean initialResponseSent;

    public QrsSseEventSink(QrsRequestContext context) {
        this.context = context;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public CompletionStage<?> send(OutboundSseEvent event) {
        if (isClosed) {
            // FIXME: check spec
            return CompletableFuture.completedFuture(null);
        }
        HttpServerResponse response = context.getContext().response();
        sendInitialResponse(response);
        CompletableFuture<?> ret = new CompletableFuture<>();
        Buffer data = serialiseData(event.getData());
        response.write(data, new Handler<AsyncResult<Void>>() {
            @Override
            public void handle(AsyncResult<Void> event) {
                if (event.failed()) {
                    ret.completeExceptionally(event.cause());
                } else {
                    ret.complete(null);
                }
            }
        });
        return ret;
    }

    private Buffer serialiseData(Object data) {
        // FIXME: use serialisers
        return Buffer.buffer("data: " + data.toString() + "\n\n");
    }

    @Override
    public void close() {
        if (isClosed)
            return;
        isClosed = true;
        // FIXME: close
        // FIXME: should we close too?
        context.getContext().response().end();
    }

    public void sendInitialResponse(HttpServerResponse response) {
        // FIXME: spec says we should flush the headers when first message is sent or when the resource method returns, whichever
        // happens first
        if (!initialResponseSent) {
            response.setStatusCode(Response.Status.OK.getStatusCode());
            response.putHeader(HttpHeaders.CONTENT_TYPE, MediaType.SERVER_SENT_EVENTS);
            response.setChunked(true);
            // FIXME: headers
            initialResponseSent = true;

            response.closeHandler(v -> {
                System.err.println("Server connection closed");
            });
            // FIXME: how do we send it?
        }
    }

}
