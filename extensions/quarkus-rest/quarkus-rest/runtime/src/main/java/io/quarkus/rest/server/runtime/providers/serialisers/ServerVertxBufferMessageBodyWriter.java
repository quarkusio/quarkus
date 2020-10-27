package io.quarkus.rest.server.runtime.providers.serialisers;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import io.quarkus.rest.common.runtime.providers.serialisers.VertxBufferMessageBodyWriter;
import io.quarkus.rest.server.runtime.core.LazyMethod;
import io.quarkus.rest.server.runtime.core.QuarkusRestRequestContext;
import io.quarkus.rest.server.runtime.spi.QuarkusRestMessageBodyWriter;
import io.vertx.core.buffer.Buffer;

@Provider
public class ServerVertxBufferMessageBodyWriter extends VertxBufferMessageBodyWriter
        implements QuarkusRestMessageBodyWriter<Buffer> {

    @Override
    public boolean isWriteable(Class<?> type, LazyMethod target, MediaType mediaType) {
        return true;
    }

    @Override
    public void writeResponse(Buffer buffer, QuarkusRestRequestContext context) throws WebApplicationException {
        context.getHttpServerResponse().end(buffer);
    }
}
