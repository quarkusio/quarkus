package io.quarkus.rest.server.runtime;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.LazyMethod;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveMessageBodyWriter;

import io.quarkus.resteasy.reactive.common.runtime.VertxBufferMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

@Provider
public class ServerVertxBufferMessageBodyWriter extends VertxBufferMessageBodyWriter
        implements ResteasyReactiveMessageBodyWriter<Buffer> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, ResteasyReactiveRequestContext context) throws WebApplicationException {
        context.serverResponse().end(buffer.getBytes());
    }
}
