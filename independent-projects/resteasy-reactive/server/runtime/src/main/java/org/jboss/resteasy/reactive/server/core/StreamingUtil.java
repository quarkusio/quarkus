package org.jboss.resteasy.reactive.server.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;
import org.jboss.resteasy.reactive.common.core.Serialisers;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;

// FIXME: we need to refactor the serialisation of entities to bytes between here and Sse and Serialisers
// and figure out where interceptors come into play
public class StreamingUtil {

    public static CompletionStage<?> send(ResteasyReactiveRequestContext context, Object entity, String prefix) {
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
        setHeaders(context, response);
        if (prefix != null) {
            byte[] prefixBytes = prefix.getBytes(StandardCharsets.US_ASCII);
            byte[] prefixedData = new byte[prefixBytes.length + data.length];
            System.arraycopy(prefixBytes, 0, prefixedData, 0, prefixBytes.length);
            System.arraycopy(data, 0, prefixedData, prefixBytes.length, data.length);
            data = prefixedData;
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
            throw new IllegalStateException(
                    "Could not find MessageBodyWriter for " + entityClass + " / " + entityType + " as " + mediaType);
        }
        return baos.toByteArray();
    }

    public static void setHeaders(ResteasyReactiveRequestContext context, ServerHttpResponse response) {
        // FIXME: spec says we should flush the headers when first message is sent or when the resource method returns, whichever
        // happens first
        if (!response.headWritten()) {
            if (response.getStatusCode() == 0) {
                response.setStatusCode(Response.Status.OK.getStatusCode());
            }
            response.setResponseHeader(HttpHeaders.CONTENT_TYPE, context.getResponseContentType().toString());
            response.setChunked(true);
        }
    }
}
