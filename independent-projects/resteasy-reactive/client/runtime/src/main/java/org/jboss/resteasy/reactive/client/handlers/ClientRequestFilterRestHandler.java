package org.jboss.resteasy.reactive.client.handlers;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.ClientRequestFilter;
import org.jboss.resteasy.reactive.client.impl.ClientRequestContextImpl;
import org.jboss.resteasy.reactive.client.impl.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

public class ClientRequestFilterRestHandler implements ClientRestHandler {

    private final ClientRequestFilter filter;

    public ClientRequestFilterRestHandler(ClientRequestFilter filter) {
        this.filter = filter;
    }

    @Override
    public void handle(RestClientRequestContext context) throws Exception {
        ClientRequestContextImpl requestContext = context.getOrCreateClientRequestContext();
        if (requestContext.isAborted()) {
            return;
        }
        try {
            filter.filter(requestContext);
        } catch (Exception x) {
            if (x.getMessage() != null) {
                throw new ProcessingException(x.getMessage(), x);
            } else {
                throw new ProcessingException(x);
            }
        }
        if (requestContext.getAbortedWith() != null) {
            context.setResponseStatus(requestContext.getAbortedWith().getStatus());
            context.setResponseHeaders(requestContext.getAbortedWith().getStringHeaders());
            context.setResponseReasonPhrase(requestContext.getAbortedWith().getStatusInfo().getReasonPhrase());
        }
    }
}
