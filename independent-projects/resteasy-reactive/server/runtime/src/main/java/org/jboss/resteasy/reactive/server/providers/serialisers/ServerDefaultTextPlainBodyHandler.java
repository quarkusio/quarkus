package org.jboss.resteasy.reactive.server.providers.serialisers;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import org.jboss.resteasy.reactive.common.providers.serialisers.DefaultTextPlainBodyHandler;

@Provider
@Consumes("text/plain")
public class ServerDefaultTextPlainBodyHandler extends DefaultTextPlainBodyHandler {

    @Override
    protected void validateInput(String input) throws ProcessingException {
        if (input.isEmpty()) {
            // add an empty, non-null entity in order to ensure that the response will be used as is
            // TODO: this seems to be an edge case, but perhaps it needs to be handled by RequestDeserializeHandler?
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity("").build());
        }
    }
}
