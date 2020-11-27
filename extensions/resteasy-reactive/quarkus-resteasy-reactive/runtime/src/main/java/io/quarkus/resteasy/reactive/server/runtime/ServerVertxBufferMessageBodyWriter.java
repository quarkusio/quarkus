package io.quarkus.resteasy.reactive.server.runtime;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyWriter;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

import io.quarkus.resteasy.reactive.common.runtime.VertxBufferMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

@Provider
public class ServerVertxBufferMessageBodyWriter extends VertxBufferMessageBodyWriter
        implements ServerMessageBodyWriter<Buffer> {

    @Override
    public boolean isWriteable(Class<?> type, ResteasyReactiveResourceInfo target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, ServerRequestContext context) throws WebApplicationException {
        context.serverResponse().end(buffer.getBytes());
    }
}
