package org.jboss.resteasy.reactive.server.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.SseEvent;

import org.jboss.resteasy.reactive.common.util.CommonSseUtil;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.jaxrs.OutboundSseEventImpl;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

@SuppressWarnings("ForLoopReplaceableByForEach")
public class SseUtil extends CommonSseUtil {

    private static final String NL = "\n";

    public static CompletionStage<?> send(ResteasyReactiveRequestContext context, OutboundSseEvent event,
            List<PublisherResponseHandler.StreamingResponseCustomizer> customizers) {
        ServerHttpResponse response = context.serverResponse();
        if (response.closed()) {
            // FIXME: check spec
            return CompletableFuture.completedFuture(null);
        }
        String data;
        try {
            data = serialiseEvent(context, event);
        } catch (IOException e) {
            CompletableFuture<Void> ret = new CompletableFuture<>();
            ret.completeExceptionally(e);
            return ret;
        }
        setHeaders(context, response, customizers);
        return response.write(data.getBytes(StandardCharsets.UTF_8));
    }

    private static String serialiseEvent(ResteasyReactiveRequestContext context, OutboundSseEvent event)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        MediaType eventMediaType = null;
        // NOT IN SPEC
        if (event instanceof OutboundSseEventImpl && ((OutboundSseEventImpl) event).isMediaTypeSet()) {
            eventMediaType = event.getMediaType();
            serialiseField(context, sb, "content-type", eventMediaType.toString(), false);
        }
        if (event.getComment() != null) {
            // empty field name
            serialiseField(context, sb, "", event.getComment(), false);
        }
        if (event.getId() != null) {
            serialiseField(context, sb, "id", event.getId(), false);
        }
        if (event.getName() != null) {
            serialiseField(context, sb, "event", event.getName(), false);
        }
        if (event.getReconnectDelay() != SseEvent.RECONNECT_NOT_SET) {
            // SSE spec says to ignore any non-ASCII digits (including negative) so just ignore instead of throwing
            if (event.getReconnectDelay() >= 0)
                serialiseField(context, sb, "retry", Long.toString(event.getReconnectDelay()), false);
        }
        if (event.getData() != null) {
            String data = serialiseDataToString(context, event, eventMediaType);
            serialiseField(context, sb, "data", data, true);
        }
        sb.append(NL);
        // return a UTF8 buffer
        return sb.toString();
    }

    private static void serialiseField(ResteasyReactiveRequestContext context, StringBuilder sb, String field, String value,
            boolean multiLine) {
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

    private static String serialiseDataToString(ResteasyReactiveRequestContext context, OutboundSseEvent event,
            MediaType eventMediaType)
            throws IOException {
        ServerSerialisers serialisers = context.getDeployment().getSerialisers();
        Object entity = event.getData();
        Class<?> entityClass = event.getType();
        Type entityType = event.getGenericType();
        MediaType mediaType = eventMediaType != null ? eventMediaType : context.getTarget().getStreamElementType();
        if (mediaType == null) {
            mediaType = MediaType.TEXT_PLAIN_TYPE;
        }
        // FIXME: this should belong somewhere else as it's generic
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object>[] writers = (MessageBodyWriter<Object>[]) serialisers
                .findWriters(null, entityClass, mediaType)
                .toArray(ServerSerialisers.NO_WRITER);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        boolean wrote = false;
        for (MessageBodyWriter<Object> writer : writers) {
            if (writer.isWriteable(entityClass, entityType, context.getAllAnnotations(), mediaType)) {
                // FIXME: spec doesn't really say what headers we should use here
                writer.writeTo(entity, entityClass, entityType, context.getAllAnnotations(), mediaType,
                        new QuarkusMultivaluedHashMap<>(), baos);
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

    public static void setHeaders(ResteasyReactiveRequestContext context, ServerHttpResponse response) {
        setHeaders(context, response, Collections.emptyList());
    }

    public static void setHeaders(ResteasyReactiveRequestContext context, ServerHttpResponse response,
            List<PublisherResponseHandler.StreamingResponseCustomizer> customizers) {
        if (!response.headWritten()) {
            response.setStatusCode(Response.Status.OK.getStatusCode());
            response.setResponseHeader(HttpHeaders.CONTENT_TYPE, MediaType.SERVER_SENT_EVENTS);
            if (context.getTarget().getStreamElementType() != null) {
                response.setResponseHeader(SSE_CONTENT_TYPE, context.getTarget().getStreamElementType().toString());
            }
            response.setChunked(true);
            for (int i = 0; i < customizers.size(); i++) {
                customizers.get(i).customize(response);
            }
        }
    }
}
