package org.jboss.resteasy.reactive.client.handlers;

import java.io.IOException;
import javax.ws.rs.core.MultivaluedMap;
import org.jboss.resteasy.reactive.client.impl.ClientResponseBuilderImpl;
import org.jboss.resteasy.reactive.client.impl.ClientResponseContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;
import org.jboss.resteasy.reactive.common.jaxrs.ResponseImpl;

public class ClientResponseCompleteRestHandler implements ClientRestHandler {

    @Override
    public void handle(RestClientRequestContext context) throws Exception {
        context.getResult().complete(mapToResponse(context, true));
    }

    public static ResponseImpl mapToResponse(RestClientRequestContext context, boolean parseContent)
            throws IOException {
        ClientResponseContextImpl responseContext = context.getOrCreateClientResponseContext();
        ClientResponseBuilderImpl builder = new ClientResponseBuilderImpl();
        builder.status(responseContext.getStatus(), responseContext.getReasonPhrase());
        builder.setAllHeaders(responseContext.getHeaders());
        builder.invocationState(context);
        if (context.isResponseTypeSpecified() && parseContent) { // this case means that a specific response type was requested
            Object entity = context.readEntity(responseContext.getEntityStream(),
                    context.getResponseType(),
                    responseContext.getMediaType(),
                    // FIXME: we have strings, it wants objects, perhaps there's
                    // an Object->String conversion too many
                    (MultivaluedMap) responseContext.getHeaders());
            if (entity != null) {
                builder.entity(entity);
            }
        } else {
            // in this case no specific response type was requested so we just prepare the stream
            // the users of the response are meant to use readEntity
            builder.entityStream(responseContext.getEntityStream());
        }
        return builder.build();
    }
}
