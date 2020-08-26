package io.quarkus.qrs.runtime.jaxrs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.sse.OutboundSseEvent;
import javax.ws.rs.sse.SseEvent;
import javax.ws.rs.sse.SseEventSink;

import io.quarkus.qrs.runtime.core.QrsRequestContext;
import io.quarkus.qrs.runtime.core.Serialisers;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

public class QrsSseEventSink implements SseEventSink {

    private static final String NL = "\n";
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
        Buffer data;
        try {
            data = serialiseEvent(event);
        } catch (IOException e) {
            ret.completeExceptionally(e);
            return ret;
        }
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

    private Buffer serialiseEvent(OutboundSseEvent event) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (event.getComment() != null) {
            // empty field name
            serialiseField(sb, "", event.getComment(), false);
        }
        if (event.getId() != null) {
            serialiseField(sb, "id", event.getId(), false);
        }
        if (event.getName() != null) {
            serialiseField(sb, "event", event.getName(), false);
        }
        if (event.getReconnectDelay() != SseEvent.RECONNECT_NOT_SET) {
            // SSE spec says to ignore any non-ASCII digits (including negative) so just ignore instead of throwing
            if (event.getReconnectDelay() >= 0)
                serialiseField(sb, "retry", Long.toString(event.getReconnectDelay()), false);
        }
        String data = serialiseDataToString(event);
        serialiseField(sb, "data", data, true);
        sb.append(NL);
        // return a UTF8 buffer
        return Buffer.buffer(sb.toString());
    }

    private void serialiseField(StringBuilder sb, String field, String value, boolean multiLine) {
        sb.append(field).append(":");
        // if not multi-line, just ignore whatever is after any \n, \r or \n\r, which ever comes first
        int n = value.indexOf('\n');
        int r = value.indexOf('\r');
        if (n == -1 && r == -1) {
            sb.append(value).append(NL);
        } else if (!multiLine) {
            sb.append(value, 0, Math.max(n, r)).append(NL);
        } else {
            // turn all \n \r \n\r into multiple lines
            char[] chars = value.toCharArray();
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '\n') {
                    // start a new line
                    sb.append(NL).append(field).append(":");
                    if (i + 1 < chars.length && chars[i + 1] == '\r') {
                        // eat both
                        i++;
                    }
                } else if (c == '\r') {
                    // start a new line
                    sb.append(NL).append(field).append(":");
                } else {
                    // safe to append
                    sb.append(c);
                }
            }
            // end of the line
            sb.append(NL);
        }

    }

    private String serialiseDataToString(OutboundSseEvent event) throws IOException {
        Serialisers serialisers = context.getDeployment().getSerialisers();
        Object entity = event.getData();
        Class<?> entityClass = event.getType();
        Type entityType = event.getGenericType();
        MediaType mediaType = event.getMediaType();
        // FIXME: this should belong somewhere else as it's generic
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object>[] writers = (MessageBodyWriter<Object>[]) serialisers.findWriters(entityClass, mediaType)
                .toArray(Serialisers.NO_WRITER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean wrote = false;
        for (MessageBodyWriter<Object> writer : writers) {
            // Spec(API) says we should use class/type/mediaType but doesn't talk about annotations 
            if (writer.isWriteable(entityClass, entityType, Serialisers.NO_ANNOTATION, mediaType)) {
                // FIXME: spec doesn't really say what headers we should use here
                writer.writeTo(entity, entityClass, entityType, Serialisers.NO_ANNOTATION, mediaType,
                        Serialisers.EMPTY_MULTI_MAP, baos);
                wrote = true;
                break;
            }
        }
        if (!wrote) {
            throw new IllegalStateException("Could not find MessageBodyWriter for " + entityClass);
        }
        // At this point we can only pray that the resulting output is a UTF-8 string
        return baos.toString(StandardCharsets.UTF_8.name());
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
