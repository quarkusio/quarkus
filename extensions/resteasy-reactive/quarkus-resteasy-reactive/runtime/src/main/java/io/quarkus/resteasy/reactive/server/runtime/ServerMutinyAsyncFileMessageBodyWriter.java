package io.quarkus.resteasy.reactive.server.runtime;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

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
        ResteasyReactiveRequestContext ctx = ((ResteasyReactiveRequestContext) context);
        ctx.suspend();
        ServerHttpResponse response = context.serverResponse();
        // this is only set by nice people, unfortunately
        if (file.getReadLength() != Long.MAX_VALUE) {
            response.setResponseHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(file.getReadLength()));
        } else {
            response.setChunked(true);
        }
        file.handler(buffer -> {
            try {
                response.write(buffer.getBytes());
            } catch (Exception x) {
                // believe it or not, this throws
                ctx.resume(x);
                return;
            }
            if (response.isWriteQueueFull()) {
                file.pause();
                response.addDrainHandler(file::resume);
            }
        });

        file.endHandler(new Runnable() {
            @Override
            public void run() {
                file.close();
                response.end();
                // Not sure if I need to resume, actually
                ctx.resume();
            }
        });
    }

    @Override
    public void writeTo(AsyncFile asyncFile, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        throw new UnsupportedOperationException("Returning an AsyncFile is not supported with WriterInterceptors");
    }
}
