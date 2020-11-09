package org.jboss.resteasy.reactive.server.providers.serialisers;

import io.vertx.core.buffer.Buffer;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.VertxBufferMessageBodyWriter;
import org.jboss.resteasy.reactive.server.core.LazyMethod;
import org.jboss.resteasy.reactive.server.core.ResteasyReactiveRequestContext;
import org.jboss.resteasy.reactive.server.spi.QuarkusRestMessageBodyWriter;

@Provider
public class ServerVertxBufferMessageBodyWriter extends VertxBufferMessageBodyWriter
        implements QuarkusRestMessageBodyWriter<Buffer> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, ResteasyReactiveRequestContext context) throws WebApplicationException {
        context.getHttpServerResponse().end(buffer);
    }
}
