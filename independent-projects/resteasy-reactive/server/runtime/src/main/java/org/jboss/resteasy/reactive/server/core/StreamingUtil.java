package org.jboss.resteasy.reactive.server.core;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import jakarta.ws.rs.RuntimeType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.MessageBodyWriter;

import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.jboss.resteasy.reactive.server.StreamingOutputStream;
import org.jboss.resteasy.reactive.server.handlers.PublisherResponseHandler;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

// FIXME: we need to refactor the serialisation of entities to bytes between here and Sse and Serialisers
// and figure out where interceptors come into play
@SuppressWarnings("ForLoopReplaceableByForEach")
public class StreamingUtil {

    public static CompletionStage<?> send(ResteasyReactiveRequestContext context,
            List<PublisherResponseHandler.StreamingResponseCustomizer> customizers, Object entity, String prefix,
            String suffix) {
        ServerHttpResponse response = context.serverResponse();
        if (response.closed()) {
            // FIXME: check spec
            return CompletableFuture.completedFuture(null);
        }
        byte[] data;
        try {
            data = serialiseEntity(context, entity);
        } catch (Exception e) {
            CompletableFuture<?> ret = new CompletableFuture<>();
            ret.completeExceptionally(e);
            return ret;
        }
        setHeaders(context, response, customizers);
        if (prefix != null) {
            byte[] prefixBytes = prefix.getBytes(StandardCharsets.US_ASCII);
            byte[] prefixedData = new byte[prefixBytes.length + data.length];
            System.arraycopy(prefixBytes, 0, prefixedData, 0, prefixBytes.length);
            System.arraycopy(data, 0, prefixedData, prefixBytes.length, data.length);
            data = prefixedData;
        }
        if (suffix != null) {
            byte[] suffixBytes = suffix.getBytes(StandardCharsets.US_ASCII);
            byte[] suffixedData = new byte[data.length + suffixBytes.length];
            System.arraycopy(data, 0, suffixedData, 0, data.length);
            System.arraycopy(suffixBytes, 0, suffixedData, data.length, suffixBytes.length);
            data = suffixedData;
        }
        return response.write(data);
    }

    private static byte[] serialiseEntity(ResteasyReactiveRequestContext context, Object entity) throws IOException {
        ServerSerialisers serialisers = context.getDeployment().getSerialisers();
        Class<?> entityClass = entity.getClass();
        Type entityType = context.getGenericReturnType();
        MediaType mediaType = context.getResponseMediaType();
        // FIXME: this should belong somewhere else as it's generic
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object>[] writers = (MessageBodyWriter<Object>[]) serialisers
                .findWriters(null, entityClass, mediaType, RuntimeType.SERVER)
                .toArray(ServerSerialisers.NO_WRITER);
        StreamingOutputStream baos = new StreamingOutputStream();
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
            throw new IllegalStateException(
                    "Could not find MessageBodyWriter for " + entityClass + " / " + entityType + " as " + mediaType);
        }
        return baos.toByteArray();
    }

    public static void setHeaders(ResteasyReactiveRequestContext context, ServerHttpResponse response,
            List<PublisherResponseHandler.StreamingResponseCustomizer> customizers) {
        // FIXME: spec says we should flush the headers when first message is sent or when the resource method returns, whichever
        // happens first
        if (!response.headWritten()) {
            response.setStatusCode(Response.Status.OK.getStatusCode());
            response.setResponseHeader(HttpHeaders.CONTENT_TYPE, context.getResponseContentType().toString());
            response.setChunked(true);
            for (int i = 0; i < customizers.size(); i++) {
                customizers.get(i).customize(response);
            }
        }
    }
}
