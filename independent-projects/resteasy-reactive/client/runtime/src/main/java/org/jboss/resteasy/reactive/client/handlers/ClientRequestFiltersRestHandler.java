package org.jboss.resteasy.reactive.client.handlers;

import java.util.List;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientRequestFilter;
import org.jboss.resteasy.reactive.client.QuarkusRestClientRequestContext;
import org.jboss.resteasy.reactive.client.RestClientRequestContext;
import org.jboss.resteasy.reactive.client.spi.ClientRestHandler;

public class ClientRequestFiltersRestHandler implements ClientRestHandler {
    @Override
    public void handle(RestClientRequestContext context) throws Exception {

        List<ClientRequestFilter> filters = context.getConfiguration().getRequestFilters();
        if (!filters.isEmpty()) {
            QuarkusRestClientRequestContext requestContext = context.getOrCreateClientRequestContext();
            for (ClientRequestFilter filter : filters) {
                try {
                    filter.filter(requestContext);
                } catch (Exception x) {
                    throw new ProcessingException(x);
                }
                if (requestContext.getAbortedWith() != null) {
                    context.setResponseStatus(requestContext.getAbortedWith().getStatus());
                    context.setResponseHeaders(requestContext.getAbortedWith().getStringHeaders());
                    context.setResponseReasonPhrase(requestContext.getAbortedWith().getStatusInfo().getReasonPhrase());
                    return;
                }
            }
        }
    }
}
