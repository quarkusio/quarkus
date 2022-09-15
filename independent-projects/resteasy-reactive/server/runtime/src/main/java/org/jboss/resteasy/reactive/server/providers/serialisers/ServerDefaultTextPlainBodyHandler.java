package org.jboss.resteasy.reactive.server.providers.serialisers;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;
import java.lang.reflect.Type;
import org.jboss.resteasy.reactive.common.providers.serialisers.DefaultTextPlainBodyHandler;
import org.jboss.resteasy.reactive.server.spi.ResteasyReactiveResourceInfo;
import org.jboss.resteasy.reactive.server.spi.ServerMessageBodyReader;
import org.jboss.resteasy.reactive.server.spi.ServerRequestContext;

@Provider
@Consumes("text/plain")
public class ServerDefaultTextPlainBodyHandler extends DefaultTextPlainBodyHandler implements ServerMessageBodyReader<Object> {

    @Override
    protected void validateInput(String input) throws ProcessingException {
        if (input.isEmpty()) {
            // add an empty, non-null entity in order to ensure that the response will be used as is
            // TODO: this seems to be an edge case, but perhaps it needs to be handled by RequestDeserializeHandler?
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("").build());
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, ResteasyReactiveResourceInfo lazyMethod,
            MediaType mediaType) {
        return super.isReadable(type, genericType, null, mediaType);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, MediaType mediaType, ServerRequestContext context)
            throws WebApplicationException, IOException {
        return doReadFrom(type, mediaType, context.getInputStream());
    }
}
