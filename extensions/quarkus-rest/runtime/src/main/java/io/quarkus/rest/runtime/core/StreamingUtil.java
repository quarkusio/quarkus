package io.quarkus.rest.runtime.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyWriter;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerResponse;

// FIXME: we need to refactor the serialisation of entities to bytes between here and Sse and Serialisers
// and figure out where interceptors come into play
public class StreamingUtil {

    public static CompletionStage<?> send(QuarkusRestRequestContext context, Object entity) {
        if (context.getContext().response().closed()) {
            // FIXME: check spec
            return CompletableFuture.completedFuture(null);
        }
        HttpServerResponse response = context.getContext().response();
        CompletableFuture<?> ret = new CompletableFuture<>();
        Buffer data;
        try {
            data = serialiseEntity(context, entity);
        } catch (Exception e) {
            ret.completeExceptionally(e);
            return ret;
        }
        setHeaders(context, response);
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

    private static Buffer serialiseEntity(QuarkusRestRequestContext context, Object entity) throws IOException {
        Serialisers serialisers = context.getDeployment().getSerialisers();
        Class<?> entityClass = entity.getClass();
        Type entityType = context.getGenericReturnType();
        MediaType mediaType = context.getResponseContentMediaType();
        // FIXME: this should belong somewhere else as it's generic
        @SuppressWarnings("unchecked")
        MessageBodyWriter<Object>[] writers = (MessageBodyWriter<Object>[]) serialisers
                .findWriters(null, entityClass, mediaType, RuntimeType.SERVER)
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
            throw new IllegalStateException(
                    "Could not find MessageBodyWriter for " + entityClass + " / " + entityType + " as " + mediaType);
        }
        return Buffer.buffer(baos.toByteArray());
    }

    public static void setHeaders(QuarkusRestRequestContext context, HttpServerResponse response) {
        // FIXME: spec says we should flush the headers when first message is sent or when the resource method returns, whichever
        // happens first
        if (!response.headWritten()) {
            response.setStatusCode(Response.Status.OK.getStatusCode());
            response.putHeader(HttpHeaders.CONTENT_TYPE, context.getResponseContentType().toString());
            response.setChunked(true);
            // FIXME: other headers?
        }
    }
}
