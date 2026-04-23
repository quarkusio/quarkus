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
import io.vertx.core.file.AsyncFile;
import io.vertx.core.http.HttpServerResponse;

@Provider
public class ServerVertxAsyncFileMessageBodyWriter implements ServerMessageBodyWriter<AsyncFile> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        // allow for subtypes, such as AsyncFileImpl
        return AsyncFile.class.isAssignableFrom(type);
    }

    @Override
    public void writeResponse(AsyncFile file, Type genericType, ServerRequestContext context) throws WebApplicationException {
        VertxResteasyReactiveRequestContext ctx = (VertxResteasyReactiveRequestContext) context;
        ctx.suspend();
        ServerHttpResponse response = context.serverResponse();
        if (file.getReadLength() != Long.MAX_VALUE) {
            response.setResponseHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getReadLength()));
        } else {
            response.setChunked(true);
        }

        HttpServerResponse httpResponse = ctx.vertxServerResponse();

        file.pipe()
                .endOnComplete(true)
                .to(httpResponse)
                .onComplete(new Handler<AsyncResult<Void>>() {
                    @Override
                    public void handle(AsyncResult<Void> ar) {
                        file.close();
                        if (ar.succeeded()) {
                            ctx.resume();
                        } else {
                            ctx.resume(ar.cause());
                        }
                    }
                });
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return AsyncFile.class.isAssignableFrom(type);
    }

    @Override
    public void writeTo(AsyncFile asyncFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        throw new UnsupportedOperationException("not supported");
    }

}
