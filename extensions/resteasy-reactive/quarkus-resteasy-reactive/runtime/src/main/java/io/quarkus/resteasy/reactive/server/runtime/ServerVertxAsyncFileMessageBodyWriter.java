package io.quarkus.resteasy.reactive.server.runtime;

import java.lang.reflect.Type;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerHttpResponse;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.resteasy.reactive.common.runtime.VertxAsyncFileMessageBodyWriter;
import io.vertx.core.Handler;
import io.vertx.core.file.AsyncFile;

@Provider
public class ServerVertxAsyncFileMessageBodyWriter extends VertxAsyncFileMessageBodyWriter
        implements ServerMessageBodyWriter<AsyncFile> {

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo target, MediaType mediaType) {
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
                response.addDrainHandler(new Runnable() {
                    @Override
                    public void run() {
                        file.resume();
                    }
                });
            }
        });

        file.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                file.close();
                response.end();
                // Not sure if I need to resume, actually
                ctx.resume();
            }
        });
    }
}
