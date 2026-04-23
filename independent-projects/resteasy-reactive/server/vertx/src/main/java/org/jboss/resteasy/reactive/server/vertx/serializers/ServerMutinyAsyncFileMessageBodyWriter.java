package org.jboss.resteasy.reactive.server.vertx.serializers;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;
import org.jboss.resteasy.reactive.server.vertx.VertxResteasyReactiveRequestContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.mutiny.core.file.AsyncFile;

@Provider
public class ServerMutinyAsyncFileMessageBodyWriter implements ServerMessageBodyWriter<AsyncFile> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return isWritable(type);
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isWritable(type);
    }

    private boolean isWritable(Class<?> type) {
        // allow for subtypes, such as AsyncFileImpl
        return AsyncFile.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(AsyncFile file, Type genericType, ServerRequestContext context) throws WebApplicationException {
        VertxResteasyReactiveRequestContext ctx = (VertxResteasyReactiveRequestContext) context;
        ctx.suspend();
        ServerHttpResponse response = context.serverResponse();

        io.vertx.core.file.AsyncFile delegate = file.getDelegate();
        if (delegate.getReadLength() != Long.MAX_VALUE) {
            response.setResponseHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(delegate.getReadLength()));
        } else {
            response.setChunked(true);
        }

        HttpServerResponse httpResponse = ctx.vertxServerResponse();

        delegate.pipe()
                .endOnComplete(true)
                .to(httpResponse)
                .onComplete(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        delegate.close();
                        if (ar.succeeded()) {
                            ctx.resume();
                        } else {
                            ctx.resume(ar.cause());
                        }
                    }
                });
    }

    @Override
    public void writeTo(AsyncFile asyncFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        throw new UnsupportedOperationException("Returning an AsyncFile is not supported with WriterInterceptors");
    }
}
