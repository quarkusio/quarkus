package org.jboss.resteasy.reactive.server.providers.serialisers;

import java.io.IOException;
import java.lang.reflect.Type;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
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
