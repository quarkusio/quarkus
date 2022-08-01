package org.jboss.resteasy.reactive.client.handlers;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientResponseFilter;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

public class ClientResponseFilterRestHandler implements ClientRestHandler {

    private final ClientResponseFilter filter;

    public ClientResponseFilterRestHandler(ClientResponseFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handle(RestClientRequestContext requestContext) throws Exception {
        try {
            filter.filter(requestContext.getClientRequestContext(), requestContext.getOrCreateClientResponseContext());
        } catch (WebApplicationException | ProcessingException x) {
            throw x;
        } catch (Exception x) {
            throw new ProcessingException(x);
        }
    }
}
